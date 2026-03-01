package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.grpc.NotificationsGrpcClient;
import az.fitnest.identity.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final NotificationsGrpcClient notificationsGrpcClient;

    @Override
    public void sendSms(String to, String message) {
        notificationsGrpcClient.sendSms(to, message);
    }
}
