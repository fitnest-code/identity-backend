package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.repository.UserRepository;
import az.fitnest.identity.service.UserProfileGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TestUserHelper {

    private final UserRepository userRepository;
    private final UserProfileGrpcClient userProfileGrpcClient;

    public boolean isTestUser(User user) {
        return user != null && user.isTestUser();
    }

    public boolean isTestUserId(Long userId) {
        if (userId == null) return false;
        return userRepository.findById(userId)
                .map(User::isTestUser)
                .orElse(false);
    }

    public boolean isTestIdentifier(String identifier) {
        if (identifier == null) return false;
        
        // Try by mobile
        Optional<User> userByMobile = userRepository.findFirstByMobile(identifier);
        if (userByMobile.isPresent() && userByMobile.get().isTestUser()) {
            return true;
        }
        
        // Try by email
        if (identifier.contains("@")) {
            try {
                var profile = userProfileGrpcClient.getUserByEmail(identifier);
                if (profile != null) {
                    Optional<User> userByEmail = userRepository.findById(profile.userId());
                    if (userByEmail.isPresent() && userByEmail.get().isTestUser()) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        
        return false;
    }
}
