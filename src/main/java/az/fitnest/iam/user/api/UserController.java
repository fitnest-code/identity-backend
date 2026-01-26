package az.fitnest.iam.user.api;

import az.fitnest.iam.user.adapter.service.UserService;
import az.fitnest.iam.user.api.dto.request.UpdateUserProfileRequest;
import az.fitnest.iam.user.api.dto.response.UserResponse;
import az.fitnest.iam.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody UpdateUserProfileRequest request) {
        User user = userService.updateUserProfile(userId, request.getFullName(), request.getEmail());
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{userId}/profile-image")
    public ResponseEntity<UserResponse> updateProfileImage(
            @PathVariable Long userId,
            @RequestBody UpdateProfileImageRequest request) {
        User user = userService.updateProfileImageUrl(userId, request.getImageUrl());
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{userId}/setup-required")
    public ResponseEntity<UserResponse> updateSetupRequired(
            @PathVariable Long userId,
            @RequestBody UpdateSetupRequiredRequest request) {
        User user = userService.updateSetupRequired(userId, request.getSetupRequired());
        return ResponseEntity.ok(toUserResponse(user));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(String.valueOf(user.getId()))
                .fullName(user.getFullName())
                .mobile(user.getMobile())
                .email(user.getEmail())
                .hasAccount(user.getHasAccount())
                .setupRequired(user.getSetupRequired())
                .profileImageUrl(user.getProfileImageUrl())
                .language(user.getLanguage() != null ? user.getLanguage().name() : null)
                .createdAt(user.getCreatedDate())
                .build();
    }

    @lombok.Data
    public static class UpdateProfileImageRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("image_url")
        private String imageUrl;
    }

    @lombok.Data
    public static class UpdateSetupRequiredRequest {
        @com.fasterxml.jackson.annotation.JsonProperty("setup_required")
        private Boolean setupRequired;
    }
}
