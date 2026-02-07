package az.fitnest.iam.messaging;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Implementation of EmailService using JavaMailSender and Thymeleaf.
 * All methods are executed asynchronously to prevent blocking the main thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${mail.from:no-reply@fitnest.az}")
    private String fromAddress;

    @Override
    @Async("taskExecutor")
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        log.debug("Sending HTML email to {} with template {}", to, templateName);
        try {
            Context context = new Context();
            context.setVariables(variables);
            
            String htmlContent = templateEngine.process("email/" + templateName, context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Successfully sent HTML email to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}", to, e);
            // In a production system, you might want to implement a retry mechanism here
        } catch (Exception e) {
            log.error("Unexpected error while preparing email for {}", to, e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendSimpleEmail(String to, String subject, String content) {
        log.debug("Sending simple email to {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.info("Successfully sent simple email to {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to {}", to, e);
        }
    }
}
