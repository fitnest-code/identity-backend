package az.fitnest.identity.controller;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.request.UpdateSessionStatusRequest;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.dto.request.UpdateUserProfileCommandRequest;
import az.fitnest.identity.dto.request.UpdateProfileImageRequest;
import az.fitnest.identity.dto.request.UpdateSetupRequiredRequest;
import az.fitnest.identity.dto.request.UpdateUserProfileRequest;
import az.fitnest.identity.mapper.UserResponseMapper;
import az.fitnest.identity.dto.response.UserResponse;
import az.fitnest.identity.model.entity.User;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@Hidden
@Tag(name = "Daxili İstifadəçi", description = "Daxili istifadəçi idarəetməsi üçün endpointlər. Sistem daxili istifadə üçün nəzərdə tutulub.")
public class InternalUserController {

    private final UserService userService;
    private final UserResponseMapper userResponseMapper;

    @GetMapping("/{userId}")
    @Operation(summary = "İstifadəçini ID ilə əldə edin", description = "İstifadəçi ID-si ilə istifadəçi məlumatlarını qaytarır.")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "İstifadəçi profilini yeniləyin", description = "İstifadəçi profilini yeniləyir.")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserProfileRequest request) {
        User user = userService.updateUserProfile(userId, new UpdateUserProfileCommandRequest(
                request.firstName(),
                request.lastName(),
                request.email(),
                null
        ));
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{userId}/profile-image")
    @Operation(summary = "Profil şəklini yeniləyin", description = "İstifadəçinin profil şəklini yeniləyir.")
    public ResponseEntity<UserResponse> updateProfileImage(
            @PathVariable Long userId,
            @RequestBody UpdateProfileImageRequest request) {
        User user = userService.updateProfileImageUrl(userId, request.imageUrl());
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{userId}/setup-required")
    @Operation(summary = "Setup statusunu yeniləyin", description = "İstifadəçinin setupRequired statusunu yeniləyir.")
    public ResponseEntity<UserResponse> updateSetupRequired(
            @PathVariable Long userId,
            @RequestBody UpdateSetupRequiredRequest request) {
        User user = userService.updateSetupRequired(userId, request.setupRequired());
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{userId}/language")
    @Operation(summary = "Dil kodunu yeniləyin", description = "İstifadəçinin dil kodunu yeniləyir.")
    public ResponseEntity<UserResponse> updateLanguage(
            @PathVariable Long userId,
            @RequestBody @Valid az.fitnest.identity.dto.request.UpdateLanguageRequest request) {
        User user = userService.updateLanguage(userId, request.code());
        return ResponseEntity.ok(toUserResponse(user));
    }

    @PutMapping("/{userId}/session-status")
    @Operation(summary = "Sessiya statusunu yeniləyin", description = "İstifadəçinin sessiya statusunu yeniləyir.")
    public ResponseEntity<UserResponse> updateSessionStatus(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateSessionStatusRequest request) {
        User user = userService.updateSessionStatus(userId, request.sessionStatus());
        return ResponseEntity.ok(toUserResponse(user));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "İstifadəçini deaktiv edin", description = "İstifadəçini deaktiv edir.")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long userId,
            @RequestParam(name = "reason", required = false) String reason) {
        userService.deactivateUser(userId, reason);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toUserResponse(User user) {
        return userResponseMapper.toResponse(user);
    }
}
