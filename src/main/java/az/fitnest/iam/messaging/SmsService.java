package az.fitnest.iam.messaging;

public interface SmsService {
    void sendSms(String to, String message);
}
