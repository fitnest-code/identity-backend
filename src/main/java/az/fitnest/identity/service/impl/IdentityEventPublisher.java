package az.fitnest.identity.service.impl;
import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
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
