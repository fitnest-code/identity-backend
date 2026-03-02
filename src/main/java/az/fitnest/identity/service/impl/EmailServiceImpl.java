package az.fitnest.identity.service.impl;

import az.fitnest.identity.grpc.NotificationsGrpcClient;
import az.fitnest.identity.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final NotificationsGrpcClient notificationsGrpcClient;

    @Override
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        Map<String, String> stringVariables = variables.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
        notificationsGrpcClient.sendHtmlEmail(to, subject, templateName, stringVariables);
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String content) {
        notificationsGrpcClient.sendEmail(to, subject, content);
    }
}
