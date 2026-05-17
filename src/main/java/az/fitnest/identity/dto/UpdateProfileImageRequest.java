package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateProfileImageRequest(
        @JsonProperty("image_url")
        String imageUrl
) {
}
