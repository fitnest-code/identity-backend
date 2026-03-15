package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateProfileImageRequest(
    @JsonProperty("image_url")
    String imageUrl
) {}
