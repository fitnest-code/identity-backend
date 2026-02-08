package az.fitnest.iam.user.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class UserResponse {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String mobile;



    @JsonProperty("has_account")
    private boolean hasAccount;

    @JsonProperty("setup_required")
    private boolean setupRequired;

    @JsonProperty("profile_image_url")
    private String profileImageUrl;

    private String language;

    @JsonProperty("account_locked")
    private boolean accountLocked;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}