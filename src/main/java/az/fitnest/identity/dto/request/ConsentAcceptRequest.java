package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConsentAcceptRequest(
    @NotBlank
    @JsonProperty("privacy_version")
    String privacyVersion,

    @NotBlank
    @JsonProperty("terms_version")
    String termsVersion,

    @NotNull
    @AssertTrue
    Boolean accepted,

    @NotBlank
    String channel,

    @NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "^(ios|android)$", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE, message = "Platform must be ios or android")
    String platform
) {}
