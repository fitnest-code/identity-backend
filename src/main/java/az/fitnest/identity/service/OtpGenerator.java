package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import java.security.SecureRandom;

public class OtpGenerator {

    private static final int OTP_LENGTH = 4;
    private static final int MAX = (int) Math.pow(10, OTP_LENGTH);

    private final SecureRandom random = new SecureRandom();

    public String generateOtp() {
        int otp = random.nextInt(MAX);
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }
}
