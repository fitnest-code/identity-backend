package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

public interface SmsService {
    void sendSms(String to, String message);
}
