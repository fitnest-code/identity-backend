package az.fitnest.identity.service.impl;

import az.fitnest.identity.grpc.NotificationsGrpcClient;
import az.fitnest.identity.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// @Service  // Temporarily disabled — notifications service is offline; using MockSmsService instead
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final NotificationsGrpcClient notificationsGrpcClient;

    @Override
    public void sendSms(String to, String message) {
        notificationsGrpcClient.sendSms(to, message);
    }
}
