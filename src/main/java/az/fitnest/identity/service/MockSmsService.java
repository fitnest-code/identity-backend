package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import org.springframework.stereotype.Service;

public class MockSmsService implements SmsService {

    @Override
    public void sendSms(String to, String message) {
    }
}
