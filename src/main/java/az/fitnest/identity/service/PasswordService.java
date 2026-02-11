package az.fitnest.identity.service;

import az.fitnest.identity.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

public interface PasswordService {
    String hashPassword(String rawPassword);
    boolean verifyPassword(String rawPassword, String passwordHash);
}
