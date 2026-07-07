package az.fitnest.identity.service;

import az.fitnest.identity.exception.ForbiddenException;
import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.entity.UserDevice;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.repository.UserDeviceRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.impl.TestUserHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RedisTokenService redisTokenService;
    private final AuthTokenRepository authTokenRepository;
    private final TestUserHelper testUserHelper;

    @Transactional
    public User validateAndBindDeviceForLogin(User user, String deviceId, String deviceType, boolean deviceIdRequired) {
        if (testUserHelper.isTestUser(user)) {
            return user;
        }
        boolean isMobile = "iOS".equalsIgnoreCase(deviceType) || "Android".equalsIgnoreCase(deviceType);
        if (!isMobile) {
            return user;
        }

        if (deviceId == null || deviceId.isBlank()) {
            if (deviceIdRequired) {
                throw new InvalidCredentialsException("error.auth.device_id_required", "error.auth.device_id_required");
            }
            if (user.getDeviceId() != null && !user.getDeviceId().isBlank()) {
                throw new ForbiddenException("error.auth.device_mismatch", "error.auth.device_mismatch");
            }
            return user;
        }

        String reqDeviceId = deviceId.trim();

        // First time device binding
        if (user.getDeviceId() == null || user.getDeviceId().isBlank()) {
            user.setDeviceId(reqDeviceId);
            User savedUser = userRepository.save(user);
            registerDevice(savedUser, reqDeviceId);
            return savedUser;
        }

        // Check if device is allowed (exists in history)
        boolean deviceAllowed = userDeviceRepository.existsByUserIdAndDeviceId(user.getId(), reqDeviceId);
        if (!deviceAllowed) {
            throw new ForbiddenException("error.auth.device_mismatch", "error.auth.device_mismatch");
        }

        // If it is allowed, update active device if it's not current
        if (!reqDeviceId.equals(user.getDeviceId())) {
            user.setDeviceId(reqDeviceId);
            user = userRepository.save(user);
        }

        return user;
    }

    @Transactional
    public User validateAndBindDeviceForVerification(User user, String deviceId, String deviceType) {
        if (testUserHelper.isTestUser(user)) {
            return user;
        }
        boolean isMobile = "iOS".equalsIgnoreCase(deviceType) || "Android".equalsIgnoreCase(deviceType);
        if (!isMobile) {
            return user;
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new InvalidCredentialsException("error.auth.device_id_required", "error.auth.device_id_required");
        }

        String reqDeviceId = deviceId.trim();

        // First time device binding
        if (user.getDeviceId() == null || user.getDeviceId().isBlank()) {
            user.setDeviceId(reqDeviceId);
            User savedUser = userRepository.save(user);
            registerDevice(savedUser, reqDeviceId);
            return savedUser;
        }

        // Check if device is already allowed (known)
        boolean deviceAllowed = userDeviceRepository.existsByUserIdAndDeviceId(user.getId(), reqDeviceId);
        if (deviceAllowed) {
            if (!reqDeviceId.equals(user.getDeviceId())) {
                user.setDeviceId(reqDeviceId);
                user = userRepository.save(user);
            }
            return user;
        }

        // Device change - check limit
        if (!testUserHelper.isTestUser(user) && user.getDeviceChangeCount() >= 1) {
            throw new ForbiddenException("error.auth.device_limit_exceeded", "error.auth.device_limit_exceeded");
        }

        // Increment count, update device
        user.setDeviceChangeCount(user.getDeviceChangeCount() + 1);
        user.setDeviceId(reqDeviceId);
        user = userRepository.save(user);

        // Register the new device in history
        registerDevice(user, reqDeviceId);

        // Revoke all existing sessions
        revokeAllUserSessions(user.getId());

        return user;
    }

    @Transactional
    public void registerDevice(User user, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        String cleanDeviceId = deviceId.trim();
        if (!userDeviceRepository.existsByUserIdAndDeviceId(user.getId(), cleanDeviceId)) {
            UserDevice userDevice = UserDevice.builder()
                    .user(user)
                    .deviceId(cleanDeviceId)
                    .createdAt(Instant.now())
                    .build();
            userDeviceRepository.save(userDevice);
            log.info("Registered device {} for user {}", cleanDeviceId, user.getId());
        }
    }

    private void revokeAllUserSessions(Long userId) {
        // Revoke all existing tokens in Redis
        redisTokenService.removeAllSessions(userId);
        redisTokenService.removeActiveSession(userId, "iOS");
        redisTokenService.removeActiveSession(userId, "Android");
        redisTokenService.removeActiveSession(userId, "Web");

        // Revoke all existing tokens in DB
        authTokenRepository.deleteByUserId(userId);
        log.info("Revoked all sessions for user {}", userId);
    }

    public boolean isDeviceKnown(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        return userDeviceRepository.existsByUserIdAndDeviceId(userId, deviceId.trim());
    }

    public boolean isNewDevice(Long userId, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        return !userDeviceRepository.existsByUserIdAndDeviceId(userId, deviceId.trim());
    }

    @Transactional
    public void resetDeviceLimit(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new az.fitnest.identity.exception.ResourceNotFoundException("error.auth.user_not_found", "error.auth.user_not_found"));
        user.setDeviceId(null);
        user.setDeviceChangeCount(0);
        userRepository.save(user);
        userDeviceRepository.deleteByUserId(userId);
        revokeAllUserSessions(userId);
        log.info("Reset device limit and cleared devices for user {}", userId);
    }

    @Transactional
    public void resetAllDeviceLimits() {
        userRepository.resetAllDeviceLimits();
        userDeviceRepository.deleteAll();
        log.info("Reset device limit for all users");
    }
}
