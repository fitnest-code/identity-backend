package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.event.NotificationEvent;
import az.fitnest.identity.service.UserSetupCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSetupCompleted(Long userId) {
        UserSetupCompletedEvent event = UserSetupCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .timestamp(System.currentTimeMillis())
                .source("identity-backend")
                .build();
        kafkaTemplate.send("user-setup-completed", String.valueOf(userId), event);
    }

    public void publishNotification(NotificationEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(System.currentTimeMillis());
        }
        log.info("Publishing notification event: {} to recipient: {} on topic: notification-events",
                event.getEventId(), event.getRecipient());
        kafkaTemplate.send("notification-events", event.getRecipient(), event);
    }
}
