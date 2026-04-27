package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AdminConsentResponse(
        Long id,

        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("privacy_policy_version")
        String privacyPolicyVersion,

        @JsonProperty("terms_of_use_version")
        String termsOfUseVersion,

        @JsonProperty("accepted_at")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
        LocalDateTime acceptedAt,

        @JsonProperty("ip_address")
        String ipAddress,

        @JsonProperty("user_agent")
        String userAgent,

        String platform
) {
}
