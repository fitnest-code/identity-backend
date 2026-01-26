package az.fitnest.iam.messaging;

public interface EmailSender {

    void sendOtp(String email, String otp, String purpose);
}