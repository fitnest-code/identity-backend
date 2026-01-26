package az.fitnest.iam.otp.adapter.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpGenerator {

    private static final int OTP_LENGTH = 4;
    private static final int MAX = (int) Math.pow(10, OTP_LENGTH);

    private final SecureRandom random = new SecureRandom();

    public String generateOtp() {
        int rand = random.nextInt(MAX);
        return String.format("%0" + OTP_LENGTH + "d", rand);
    }
}
