package az.fitnest.iam.user.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("full_name")
    private String fullName;

    private String mobile;

    private String email;

    @JsonProperty("has_account")
    private Boolean hasAccount;

    @JsonProperty("setup_required")
    private Boolean setupRequired;

    @JsonProperty("profile_image_url")
    private String profileImageUrl;

    private String language;
}