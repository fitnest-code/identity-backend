package az.fitnest.identity.service.impl;

import az.fitnest.identity.exception.ResourceNotFoundException;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.event.WelcomeBonusEligibleEvent;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.WelcomeBonusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WelcomeBonusServiceImpl implements WelcomeBonusService {

    private static final String USER_EVENTS_TOPIC = "user-events";
    private static final String EVENT_TYPE = "WELCOME_BONUS_ELIGIBLE";

    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void tryPublishWelcomeBonusEligible(User user) {
        tryPublishWelcomeBonusEligible(user, null);
    }

    @Override
    public void tryPublishWelcomeBonusEligible(User user, String email) {
        if (user == null || user.getId() == null || user.isWelcomeBonusReceived()) {
            return;
        }

        // Defer Kafka I/O until AFTER_COMMIT so registration stays fast and payment
        // can resolve the committed user when marking the bonus received.
        applicationEventPublisher.publishEvent(
                new WelcomeBonusEligibleEvent(user.getId(), blankToNull(user.getMobile()), blankToNull(email))
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publishWelcomeBonusEligibleToKafka(WelcomeBonusEligibleEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>(5);
        payload.put("eventType", EVENT_TYPE);
        payload.put("userId", event.userId());
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("phone", event.phone());
        payload.put("email", event.email());

        kafkaTemplate.send(USER_EVENTS_TOPIC, event.userId().toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for userId={}", EVENT_TYPE, event.userId(), ex);
                    } else {
                        log.info("Published {} for userId={}", EVENT_TYPE, event.userId());
                    }
                });
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

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
