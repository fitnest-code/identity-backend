package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminConsentResponse {

    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("privacy_policy_version")
    private String privacyPolicyVersion;

    @JsonProperty("terms_of_use_version")
    private String termsOfUseVersion;

    @JsonProperty("accepted_at")
    private LocalDateTime acceptedAt;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("user_agent")
    private String userAgent;

    private String platform;
}
