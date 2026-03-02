package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.event.NotificationEvent;
import az.fitnest.identity.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final IdentityEventPublisher eventPublisher;

    @Override
    public void sendSms(String to, String message) {
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationEvent.NotificationType.SMS)
                .recipient(to)
                .body(message)
                .build();
        eventPublisher.publishNotification(event);
    }
}
