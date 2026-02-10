package az.fitnest.identity.shared.messaging;

public interface SmsService {
    void sendSms(String to, String message);
}
