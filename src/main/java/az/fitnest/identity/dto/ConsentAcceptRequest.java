package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentAcceptRequest {

    @NotBlank
    @JsonProperty("privacy_version")
    private String privacyVersion;

    @NotBlank
    @JsonProperty("terms_version")
    private String termsVersion;

    @NotNull
    @AssertTrue
    private Boolean accepted;

    private String channel;

    @NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "^(ios|android)$", flags = jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE, message = "Platform must be ios or android")
    private String platform;
}
