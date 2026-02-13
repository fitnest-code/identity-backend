package az.fitnest.identity.grpc;

import az.fitnest.notifications.grpc.NotificationsServiceGrpc;
import az.fitnest.notifications.grpc.SendSMSRequest;
import az.fitnest.notifications.grpc.SendSMSResponse;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
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
                throw new RuntimeException("Failed to send SMS: " + response.getErrorMessage());
            }
            log.info("SMS sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send SMS via gRPC", e);
            throw new RuntimeException("Failed to send SMS via gRPC", e);
        }
    }
}
