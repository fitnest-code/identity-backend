package az.fitnest.iamservice.service;

public interface PasswordService {

    String hashPassword(String otp);
    boolean verifyPassword(String otp, String otpHash);
}
