package az.fitnest.iam.user.adapter.service;

import az.fitnest.iam.shared.exception.ConflictException;
import az.fitnest.iam.shared.exception.ResourceNotFoundException;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

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
                .setupRequired(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .isDeleted(false)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User updateUserProfile(Long userId, String fullName, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }

        if (email != null) {
            if (email.isBlank()) {
                user.setEmail(null);
            } else {
                if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
                    throw new ConflictException("Email already in use");
                }
                user.setEmail(email);
            }
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfileImageUrl(Long userId, String profileImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setProfileImageUrl(profileImageUrl);
        return userRepository.save(user);
    }

    @Transactional
    public User updateSetupRequired(Long userId, Boolean setupRequired) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setSetupRequired(setupRequired);
        return userRepository.save(user);
    }
}
