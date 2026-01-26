package az.fitnest.iam.user.api;

import az.fitnest.iam.shared.exception.ResourceNotFoundException;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.api.dto.response.UserResponse;
import az.fitnest.iam.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserResponse response = UserResponse.builder()
                .userId(String.valueOf(user.getId()))
                .fullName(user.getFullName())
                .mobile(user.getMobile())
                .email(user.getEmail())
                .hasAccount(user.getHasAccount())
                .setupRequired(user.getSetupRequired())
                .profileImageUrl(user.getProfileImageUrl())
                .language(user.getLanguage() != null ? user.getLanguage().name() : null)
                .build();

        return ResponseEntity.ok(response);
    }
}
