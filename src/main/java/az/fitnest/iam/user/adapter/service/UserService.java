package az.fitnest.iam.user.adapter.service;

import az.fitnest.iam.shared.exception.ConflictException;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User createNewUser(String email, String fullName, String passwordHash) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(passwordHash)
                .hasAccount(true)
                .setupRequired(false)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .isDeleted(false)
                .build();

        return userRepository.save(user);
    }
}
