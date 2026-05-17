package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import lombok.Builder;

@Builder
public record UpdateUserProfileRequest(
        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName,

        @Email(message = "Email düzgün deyil")
        String email
) {
}
