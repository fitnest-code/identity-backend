package az.fitnest.iam.user.api;

import az.fitnest.iam.user.adapter.service.UserService;
import az.fitnest.iam.user.application.command.UpdateUserProfileCommand;
import az.fitnest.iam.user.api.dto.request.UpdateProfileImageRequest;
import az.fitnest.iam.user.api.dto.request.UpdateSetupRequiredRequest;
import az.fitnest.iam.user.api.dto.request.UpdateUserProfileRequest;
import az.fitnest.iam.user.api.dto.mapper.UserResponseMapper;
import az.fitnest.iam.user.api.dto.response.UserResponse;
import az.fitnest.iam.user.domain.model.User;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal controller for user management operations.
 * 
 * <p>This controller provides internal service-to-service communication endpoints
 * for managing users. These endpoints are not exposed via the API Gateway and are
 * intended for use by other microservices only.</p>
 * 
 * <h2>Consuming Services:</h2>
 * <ul>
 *   <li><strong>user-service</strong> - Uses IamServiceClient to call these endpoints for:
 *     <ul>
 *       <li>Retrieving user information</li>
 *       <li>Updating user profiles</li>
 *       <li>Managing profile images</li>
 *       <li>Setting setup completion status</li>
 *       <li>User deletion</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h2>Endpoints:</h2>
 * <ul>
 *   <li>{@code GET /api/v1/internal/users/{userId}} - Get user by ID</li>
 *   <li>{@code PUT /api/v1/internal/users/{userId}} - Update user profile</li>
 *   <li>{@code PUT /api/v1/internal/users/{userId}/profile-image} - Update profile image URL</li>
 *   <li>{@code PUT /api/v1/internal/users/{userId}/setup-required} - Update setup required flag</li>
 *   <li>{@code DELETE /api/v1/internal/users/{userId}} - Delete user</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@Hidden // Hide from Swagger - internal endpoints only
public class InternalUserController {

    private final UserService userService;

    /**
     * Get user by ID.
     * Called by user-service to retrieve basic user info.
     * 
     * @param userId The ID of the user to retrieve
     * @return User details including email, name, and profile URL
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(toUserResponse(user));
    }

    /**
     * Update user profile contact info.
     * Called by user-service when user updates their profile.
     * 
     * @param userId The ID of the user
     * @param request The new profile details
     * @return Updated user details
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserProfileRequest request) {
        User user = userService.updateUserProfile(userId, UpdateUserProfileCommand.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .build());
        return ResponseEntity.ok(toUserResponse(user));
    }

    /**
     * Update user profile image URL.
     * Called by user-service after media upload.
     * 
     * @param userId The ID of the user
     * @param request Request containing the new image URL
     * @return Updated user details
     */
    @PutMapping("/{userId}/profile-image")
    public ResponseEntity<UserResponse> updateProfileImage(
            @PathVariable Long userId,
            @RequestBody UpdateProfileImageRequest request) {
        User user = userService.updateProfileImageUrl(userId, request.getImageUrl());
        return ResponseEntity.ok(toUserResponse(user));
    }

    /**
     * Update setup required status.
     * Called by user-service after setup completion.
     * 
     * @param userId The ID of the user
     * @param request Request containing the new status
     * @return Updated user details
     */
    @PutMapping("/{userId}/setup-required")
    public ResponseEntity<UserResponse> updateSetupRequired(
            @PathVariable Long userId,
            @RequestBody UpdateSetupRequiredRequest request) {
        User user = userService.updateSetupRequired(userId, request.getSetupRequired());
        return ResponseEntity.ok(toUserResponse(user));
    }

    /**
     * Update user language preference.
     * Called by user-service when user changes language.
     *
     * @param userId The ID of the user
     * @param request Request containing the new language
     * @return Updated user details
     */
    @PutMapping("/{userId}/language")
    public ResponseEntity<UserResponse> updateLanguage(
            @PathVariable Long userId,
            @RequestBody @Valid az.fitnest.iam.user.api.dto.request.UpdateLanguageRequest request) {
        User user = userService.updateLanguage(userId, request.getLanguage());
        return ResponseEntity.ok(toUserResponse(user));
    }

    /**
     * Delete a user.
     * Called by user-service for account deletion.
     * 
     * @param userId The ID of the user to delete
     * @param reason Optional reason for deletion
     */
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
