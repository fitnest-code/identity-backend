package az.fitnest.identity.controller;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.UpdateSessionStatusRequest;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.UpdateUserProfileCommand;
import az.fitnest.identity.dto.UpdateProfileImageRequest;
import az.fitnest.identity.dto.UpdateSetupRequiredRequest;
import az.fitnest.identity.dto.UpdateUserProfileRequest;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.model.entity.User;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@Hidden
public class InternalUserController {

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

    @PutMapping("/{userId}/language")
    public ResponseEntity<UserResponse> updateLanguage(
            @PathVariable Long userId,
            @RequestBody @Valid az.fitnest.identity.dto.UpdateLanguageRequest request) {
        User user = userService.updateLanguage(userId, request.getCode());
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{userId}/session-status")
    public ResponseEntity<UserResponse> updateSessionStatus(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateSessionStatusRequest request) {
        User user = userService.updateSessionStatus(userId, request.getSessionStatus());
        return ResponseEntity.ok(toUserResponse(user));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @RequestParam(name = "reason", required = false) String reason) {
        userService.deleteUser(userId, reason);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponseMapper.toResponse(user);
    }
}
