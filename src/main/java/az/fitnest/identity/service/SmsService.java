package az.fitnest.identity.service;

public interface SmsService {
    void sendSms(String to, String message);
}
