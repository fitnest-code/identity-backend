package az.fitnest.iam.user.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @JsonProperty("full_name")
    private String fullName;

    private String email;
}
