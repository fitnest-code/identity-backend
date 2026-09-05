package az.fitnest.identity.service.impl;

import az.fitnest.identity.exception.ResourceNotFoundException;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.UserProfileGrpcClient;
import az.fitnest.identity.service.WelcomeBonusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.kafka.core.KafkaTemplate;

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
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void tryPublishWelcomeBonusEligible(User user) {
        if (user == null || user.getId() == null || user.isWelcomeBonusReceived()) {
            return;
        }

        // Publish only after the registration transaction commits so payment can
        // resolve the user when marking welcome bonus received.
        applicationEventPublisher.publishEvent(
                new WelcomeBonusEligibleEvent(
                        user.getId(),
                        user.getMobile(),
                        resolveEmail(user.getId())
                )
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWelcomeBonusEligible(WelcomeBonusEligibleEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "WELCOME_BONUS_ELIGIBLE");
        payload.put("userId", event.userId());
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("phone", event.phone());
        payload.put("email", event.email());

        kafkaTemplate.send("user-events", event.userId().toString(), payload);
        log.info("Published WELCOME_BONUS_ELIGIBLE for userId={}", event.userId());
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

    public record WelcomeBonusEligibleEvent(Long userId, String phone, String email) {}
}
