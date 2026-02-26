package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "User profile information returned in authentication responses")
public class UserResponse {

    @JsonProperty("user_id")
    @Schema(description = "Unique identifier for the user", example = "12345")
    private Long userId;

    @JsonProperty("first_name")
    @Schema(description = "User's first name", example = "John")
    private String firstName;

    @JsonProperty("last_name")
    @Schema(description = "User's last name", example = "Doe")
    private String lastName;

    @Schema(description = "User's mobile number", example = "0501234567")
    private String mobile;

    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;

    @JsonProperty("has_account")
    @Schema(description = "Indicates if the user has a complete account setup", example = "true")
    private boolean hasAccount;

    @JsonProperty("setup_required")
    @Schema(description = "Indicates if additional profile setup is required", example = "false")
    private boolean setupRequired;

    @JsonProperty("profile_image_url")
    @Schema(description = "URL to the user's profile image", example = "https://example.com/images/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "User's preferred language code", example = "en")
    private String language;

    @Schema(description = "User account status (ACTIVE, INACTIVE, LOCKED, NO_SESSIONS)", example = "ACTIVE")
    private String status;

    @JsonProperty("account_locked")
    @Schema(description = "Indicates if the user account is locked", example = "false")
    private boolean accountLocked;

    @JsonProperty("created_at")
    @Schema(description = "Timestamp when the user account was created", example = "2023-01-15T10:30:00")
    private LocalDateTime createdAt;

    @JsonProperty("consent_required")
    @Schema(description = "Indicates if user consent is required for certain actions", example = "false")
    private boolean consentRequired;
}