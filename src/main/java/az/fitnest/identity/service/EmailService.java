package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import java.util.Map;

public interface EmailService {

    void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);

    void sendSimpleEmail(String to, String subject, String content);
}
