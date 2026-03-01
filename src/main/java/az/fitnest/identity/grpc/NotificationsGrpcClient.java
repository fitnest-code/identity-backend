package az.fitnest.identity.grpc;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.notifications.grpc.NotificationsServiceGrpc;
import az.fitnest.notifications.grpc.SendEmailResponse;
import az.fitnest.notifications.grpc.SendHtmlEmailRequest;
import az.fitnest.notifications.grpc.SendSimpleEmailRequest;
import az.fitnest.notifications.grpc.SendSMSRequest;
import az.fitnest.notifications.grpc.SendSMSResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationsGrpcClient {

    @GrpcClient("notifications-service")
    private NotificationsServiceGrpc.NotificationsServiceBlockingStub blockingStub;

    @Value("${grpc.notifications.deadline-ms:10000}")
    private long deadlineMs;

    private NotificationsServiceGrpc.NotificationsServiceBlockingStub withDeadline() {
        return blockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    public void sendSms(String to, String message) {
        SendSMSRequest request = SendSMSRequest.newBuilder()
                .setTo(to)
                .setMessage(message)
                .build();

        try {
            SendSMSResponse response = withDeadline().sendSMS(request);
            if (!response.getSuccess()) {
                // log.warn("Failed to send SMS (non-blocking): {}", response.getErrorMessage());
            } else {
                // log.info("SMS sent successfully to {}", to);
            }
        } catch (Exception e) {
            // log.warn("Failed to send SMS via gRPC (non-blocking). Using mock OTP '1111' logic. Error: {}", e.getMessage());
        }
    }

    public void sendEmail(String to, String subject, String body) {
        SendSimpleEmailRequest request = SendSimpleEmailRequest.newBuilder()
                .setTo(to)
                .setSubject(subject)
                .setBody(body)
                .build();

        try {
            SendEmailResponse response = withDeadline().sendSimpleEmail(request);
            if (!response.getSuccess()) {
                throw new RuntimeException("Failed to send email: " + response.getErrorMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email via gRPC", e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, String> variables) {
        SendHtmlEmailRequest request = SendHtmlEmailRequest.newBuilder()
                .setTo(to)
                .setSubject(subject)
                .setTemplateName(templateName)
                .putAllVariables(variables)
                .build();

        try {
            SendEmailResponse response = withDeadline().sendHtmlEmail(request);
            if (!response.getSuccess()) {
                throw new RuntimeException("Failed to send HTML email: " + response.getErrorMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send HTML email via gRPC", e);
        }
    }
}
