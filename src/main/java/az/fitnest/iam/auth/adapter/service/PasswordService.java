package az.fitnest.iam.auth.adapter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean verifyPassword(String rawPassword, String passwordHash) {
        if (passwordHash == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
