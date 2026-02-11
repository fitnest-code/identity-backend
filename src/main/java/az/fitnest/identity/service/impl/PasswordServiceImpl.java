package az.fitnest.identity.service.impl;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final PasswordEncoder passwordEncoder;

        @Override
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

        @Override
    public boolean verifyPassword(String rawPassword, String passwordHash) {
        if (passwordHash == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
