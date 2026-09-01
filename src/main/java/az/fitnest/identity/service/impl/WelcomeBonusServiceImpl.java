package az.fitnest.identity.service.impl;

import az.fitnest.identity.exception.ResourceNotFoundException;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.UserProfileGrpcClient;
import az.fitnest.identity.service.WelcomeBonusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WelcomeBonusServiceImpl implements WelcomeBonusService {

    private final UserRepository userRepository;
    private final UserProfileGrpcClient userProfileGrpcClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void tryPublishWelcomeBonusEligible(User user) {
        if (user == null || user.isWelcomeBonusReceived()) {
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WELCOME_BONUS_ELIGIBLE");
        event.put("userId", user.getId());
        event.put("timestamp", System.currentTimeMillis());
        event.put("phone", user.getMobile());
        event.put("email", resolveEmail(user.getId()));

        kafkaTemplate.send("user-events", user.getId().toString(), event);
        log.info("Published WELCOME_BONUS_ELIGIBLE for userId={}", user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWelcomeBonusReceived(Long userId) {
        return userRepository.findById(userId)
                .map(User::isWelcomeBonusReceived)
                .orElse(false);
    }

    @Override
    @Transactional
    public void markWelcomeBonusReceived(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("error.auth.user_not_found"));
        if (user.isWelcomeBonusReceived()) {
            return;
        }
        user.setWelcomeBonusReceived(true);
        userRepository.save(user);
        log.info("Marked welcome bonus received for userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findUserIdsPendingWelcomeBonus() {
        return userRepository.findUserIdsPendingWelcomeBonus();
    }

    private String resolveEmail(Long userId) {
        try {
            var profile = userProfileGrpcClient.getUserProfileDetails(userId);
            if (profile != null && profile.getEmail() != null && !profile.getEmail().isBlank()) {
                return profile.getEmail();
            }
        } catch (Exception e) {
            log.debug("Could not resolve email for welcome bonus event userId={}: {}", userId, e.getMessage());
        }
        return null;
    }
}
