package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.event.NotificationEvent;
import az.fitnest.identity.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final IdentityEventPublisher eventPublisher;
    private final az.fitnest.identity.client.NotificationsGrpcClient notificationsGrpcClient;

    @Override
    public void sendSms(String to, String message) {
        // Dispatch real SMS instantly via gRPC to the notifications-backend
        notificationsGrpcClient.sendSms(to, message);

        // Keep publishing the async event for audit/logging purposes
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationEvent.NotificationType.SMS)
                .recipient(to)
                .body(message)
                .build();
        eventPublisher.publishNotification(event);
    }
}
