package az.fitnest.identity.service;
import az.fitnest.identity.model.enums.UserStatus;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
public class IdentityEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSetupCompleted(Long userId) {
        UserSetupCompletedEvent event = UserSetupCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .source("identity-service")
                .build();
        kafkaTemplate.send("user-setup-completed", String.valueOf(userId), event);
    }
}

