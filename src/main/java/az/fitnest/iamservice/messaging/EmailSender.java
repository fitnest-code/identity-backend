package az.fitnest.iamservice.messaging;

public interface EmailSender {

    void sendOtp(String email, String otp, String purpose);
}
