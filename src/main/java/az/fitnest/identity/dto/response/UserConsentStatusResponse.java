package az.fitnest.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record UserConsentStatusResponse(
        ConsentDetail privacy,
        ConsentDetail terms,
        @JsonProperty("is_all_accepted")
        boolean isAllAccepted
) {
    public record ConsentDetail(
            boolean accepted,

            @JsonProperty("up_to_date")
            boolean upToDate,

            @JsonProperty("accepted_version")
            String acceptedVersion,

            @JsonProperty("latest_version")
            String latestVersion,

            @JsonProperty("accepted_at")
            @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
            LocalDateTime acceptedAt
    ) {
    }
}
