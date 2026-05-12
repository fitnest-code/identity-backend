package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.OtpPurpose;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpGenerator {

    private static final int OTP_LENGTH = 4;
    private static final int MAX = (int) Math.pow(10, OTP_LENGTH);

    private final SecureRandom random = new SecureRandom();

    public String generateOtp(OtpPurpose purpose) {
        int otp = random.nextInt(MAX);
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }
}
