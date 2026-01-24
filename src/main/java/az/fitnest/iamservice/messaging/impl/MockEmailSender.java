package az.fitnest.iamservice.messaging.impl;

import az.fitnest.iamservice.messaging.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("local")
public class MockEmailSender implements EmailSender {

    @Override
    public void sendOtp(String email, String otp, String purpose) {
        log.info("[MOCK EMAIL] Sending OTP to email={}, purpose={}, otp={}", email, otp, purpose);
    }
}
