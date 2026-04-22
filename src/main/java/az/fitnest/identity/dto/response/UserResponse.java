package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User profile information returned in authentication responses")
@Builder
public record UserResponse(
    @JsonProperty("user_id")
    @Schema(description = "Unique identifier for the user", example = "12345")
    Long userId,

    @JsonProperty("first_name")
    @Schema(description = "User's first name", example = "John")
    String firstName,

    @JsonProperty("last_name")
    @Schema(description = "User's last name", example = "Doe")
    String lastName,

    @Schema(description = "User's mobile number", example = "0501234567")
    String mobile,

    @Schema(description = "User's email address", example = "john.doe@example.com")
    String email,

    @JsonProperty("has_account")
    @Schema(description = "Indicates if the user has a complete account setup", example = "true")
    boolean hasAccount,

    @JsonProperty("setup_required")
    @Schema(description = "Indicates if additional profile setup is required", example = "false")
    boolean setupRequired,

    @JsonProperty("profile_image_url")
    @Schema(description = "URL to the user's profile image", example = "https://example.com/images/profile.jpg")
    String profileImageUrl,

    @Schema(description = "User's preferred language code", example = "en")
    String language,

    @Schema(description = "User account status (ACTIVE, INACTIVE, LOCKED, NO_SESSIONS)", example = "ACTIVE")
    String status,

    @JsonProperty("account_locked")
    @Schema(description = "Indicates if the user account is locked", example = "false")
    boolean accountLocked,

    @JsonProperty("created_at")
    @Schema(description = "Timestamp when the user account was created", example = "2023-01-15T10:30:00")
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
    LocalDateTime createdAt,

    @JsonProperty("consent_required")
    @Schema(description = "Indicates if user consent is required for certain actions", example = "false")
    boolean consentRequired,

    @JsonProperty("role")
    @Schema(description = "User's role", example = "ROLE_USER")
    String role,

    @JsonProperty("has_local_password")
    @Schema(description = "Indicates if the user has a local password set", example = "true")
    boolean hasLocalPassword
) {}
