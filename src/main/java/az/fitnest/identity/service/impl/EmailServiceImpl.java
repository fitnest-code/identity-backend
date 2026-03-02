package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.event.NotificationEvent;
import az.fitnest.identity.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final IdentityEventPublisher eventPublisher;

    @Override
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        Map<String, String> stringVariables = variables.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));

        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationEvent.NotificationType.EMAIL)
                .recipient(to)
                .subject(subject)
                .templateName(templateName)
                .variables(stringVariables)
                .build();

        eventPublisher.publishNotification(event);
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String content) {
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationEvent.NotificationType.EMAIL)
                .recipient(to)
                .subject(subject)
                .body(content)
                .build();

        eventPublisher.publishNotification(event);
    }
}
