package az.fitnest.identity.client;

import az.fitnest.notifications.grpc.NotificationsServiceGrpc;
import az.fitnest.notifications.grpc.SendSMSRequest;
import az.fitnest.notifications.grpc.SendSMSResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationsGrpcClient {

    @GrpcClient("notifications-backend")
    private NotificationsServiceGrpc.NotificationsServiceBlockingStub blockingStub;

    public boolean sendSms(String to, String message) {
        try {
            log.info("Sending SMS via gRPC to: {}", to);
            SendSMSRequest request = SendSMSRequest.newBuilder()
                    .setTo(to != null ? to : "")
                    .setMessage(message != null ? message : "")
                    .build();
            SendSMSResponse response = blockingStub.sendSMS(request);
            log.info("gRPC SendSMS response success: {}", response.getSuccess());
            return response.getSuccess();
        } catch (Exception e) {
            log.error("Failed to send SMS via gRPC to: {}", to, e);
            return false;
        }
    }
}
