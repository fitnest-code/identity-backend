package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest;
import az.fitnest.identity.model.entity.AuthToken;
import az.fitnest.identity.model.entity.Role;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.exception.ResourceNotFoundException;
import az.fitnest.identity.repository.AuthTokenRepository;
import az.fitnest.identity.repository.RoleRepository;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.security.RedisTokenService;
import az.fitnest.identity.service.impl.IdentityEventPublisher;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    User updateUserRole(Long userId, String roleName);

    User getUserById(Long userId);

    Page<User> getAllUsers(Pageable pageable);

    User createNewUser(String firstName, String lastName, String passwordHash, String mobile);

    User createNewUserWithFullName(String fullName, String passwordHash, String mobile);

    User updateUserProfile(Long userId, UpdateUserProfileCommandRequest command);

    User updateProfileImageUrl(Long userId, String profileImageUrl);

    User updateSetupRequired(Long userId, boolean setupRequired);

    User updateLanguage(Long userId, String language);

    void deactivateUser(Long userId, String reason);

    void deactivateAccount(Long userId, az.fitnest.identity.dto.DeactivateAccountRequest request);

    void changePassword(Long userId, String oldPassword, String newPassword, String confirmNewPassword);

    Page<az.fitnest.identity.dto.UserResponse> getAllUsersMapped(int page, int size);

    void deactivateAllUsers();

    User updateSessionStatus(Long userId, az.fitnest.identity.model.enums.SessionStatus sessionStatus);

    az.fitnest.identity.dto.OtpSendResponse requestEmailChange(Long userId, String newEmail);

    User confirmEmailChange(Long userId, String otpSessionId, String otpCode);

    az.fitnest.identity.dto.OtpSendResponse requestMobileChange(Long userId, String newMobile);

    User confirmMobileChange(Long userId, String otpSessionId, String otpCode);

    void deleteAccount(Long userId);

    void deleteRole(Long roleId);

    Page<az.fitnest.identity.dto.UserResponse> searchUsers(int page, int size, Long id, String name, String surname, String email, String mobile);

    Page<az.fitnest.identity.dto.UserResponse> searchUsersAdvanced(int page, int size, String query, Long packageID, Integer durationMonths);

    Page<az.fitnest.identity.dto.response.AdminUserResponse> getAdminUsers(int page, int size, String query, Long packageID, Integer durationMonths, String type);
}
