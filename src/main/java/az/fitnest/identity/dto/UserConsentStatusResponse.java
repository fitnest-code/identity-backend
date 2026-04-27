package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record UserConsentStatusResponse(
        ConsentStatus privacy,
        ConsentStatus terms,
        boolean accepted,

        @JsonProperty("up_to_date")
        boolean upToDate,

        String version,

        @JsonProperty("accepted_at")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "dd/MM/yyyy")
        LocalDateTime acceptedAt
) {
    public record ConsentStatus(
            boolean accepted,
            boolean upToDate
    ) {
    }
}
