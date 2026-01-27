package az.fitnest.iam.user.api;

import az.fitnest.iam.user.adapter.service.UserService;
import az.fitnest.iam.user.application.command.UpdateUserProfileCommand;
import az.fitnest.iam.user.api.dto.request.UpdateProfileImageRequest;
import az.fitnest.iam.user.api.dto.request.UpdateSetupRequiredRequest;
import az.fitnest.iam.user.api.dto.request.UpdateUserProfileRequest;
import az.fitnest.iam.user.api.dto.mapper.UserResponseMapper;
import az.fitnest.iam.user.api.dto.response.UserResponse;
import az.fitnest.iam.user.domain.model.User;
import jakarta.validation.Valid;
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
            @RequestBody @Valid UpdateUserProfileRequest request) {
        User user = userService.updateUserProfile(userId, UpdateUserProfileCommand.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .build());
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
        return UserResponseMapper.toResponse(user);
    }
}
