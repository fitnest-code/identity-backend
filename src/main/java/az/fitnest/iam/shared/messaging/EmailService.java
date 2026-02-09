package az.fitnest.iam.shared.messaging;

import java.util.Map;

/**
 * Modern interface for sending emails with support for templates and async delivery.
 */
public interface EmailService {

    /**
     * Sends a template-based email asynchronously.
     *
     * @param to           Recipient email address
     * @param subject      Email subject
     * @param templateName Name of the template (without .html extension)
     * @param variables    Map of variables to be injected into the template
     */
    void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables);

    /**
     * Sends a simple text email asynchronously.
     */
    void sendSimpleEmail(String to, String subject, String content);
}
