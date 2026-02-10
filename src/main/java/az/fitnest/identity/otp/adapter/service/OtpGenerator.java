package az.fitnest.identity.otp.adapter.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpGenerator {

    private static final int OTP_LENGTH = 4;
    private static final int MAX = (int) Math.pow(10, OTP_LENGTH);

    private final SecureRandom random = new SecureRandom();

    public String generateOtp() {
        return "1111"; // Hardcoded for now as requested
    }
}
