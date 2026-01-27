package az.fitnest.iam.user.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateProfileImageRequest {

    @JsonProperty("image_url")
    private String imageUrl;
}
