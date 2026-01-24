package az.fitnest.iamservice.service.impl;

import az.fitnest.iamservice.service.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BCryptPasswordService implements PasswordService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hashPassword(String otp) {
        return passwordEncoder.encode(otp);
    }

    @Override
    public boolean verifyPassword(String otp, String otpHash) {
        return passwordEncoder.matches(otp, otpHash);
    }
}
