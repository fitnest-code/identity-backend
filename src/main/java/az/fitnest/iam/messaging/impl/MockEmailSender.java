package az.fitnest.iam.messaging.impl;

import az.fitnest.iam.messaging.EmailSender;
import org.springframework.stereotype.Service;

@Service
public class MockEmailSender implements EmailSender {

    @Override
    public void sendOtp(String email, String otp, String purpose) {
    }
}