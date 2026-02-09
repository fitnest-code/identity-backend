package az.fitnest.iam.shared.messaging;

public interface SmsService {
    void sendSms(String to, String message);
}
