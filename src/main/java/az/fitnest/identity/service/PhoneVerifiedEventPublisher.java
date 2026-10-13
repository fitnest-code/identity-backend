package az.fitnest.identity.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes {@code user.phone.verified} consumed by order-backend promo module.
 * Payload: { userId, phoneNormalised, verifiedAt }.
 * Called on all three verification paths: OTP registration, phone added after
 * social login, phone change. Best-effort: never breaks the auth flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerifiedEventPublisher {

    public static final String TOPIC = "user.phone.verified";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPhoneVerified(Long userId, String phoneNormalised) {
        if (userId == null || phoneNormalised == null || phoneNormalised.isBlank()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("phoneNormalised", phoneNormalised);
        payload.put("verifiedAt", Instant.now().toString());
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(userId), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish user.phone.verified for userId={}", userId, ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to publish user.phone.verified for userId={}: {}", userId, e.getMessage());
        }
    }
}
