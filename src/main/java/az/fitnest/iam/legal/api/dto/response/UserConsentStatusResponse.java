package az.fitnest.iam.legal.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserConsentStatusResponse {
    private ConsentStatus privacy;
    private ConsentStatus terms;

    @Data
    @Builder
    public static class ConsentStatus {
        /**
         * True if the user has accepted ANY version of this document.
         */
        private boolean accepted;
        
        /**
         * True if the user has accepted the LATEST active version of this document.
         */
        @JsonProperty("up_to_date")
        private boolean upToDate;
        
        private String version;
        
        @JsonProperty("accepted_at")
        private LocalDateTime acceptedAt;
    }
}
