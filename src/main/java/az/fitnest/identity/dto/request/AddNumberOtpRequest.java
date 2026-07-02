package az.fitnest.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddNumberOtpRequest(
        @NotBlank(message = "Email is required")
        @JsonProperty("email")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^(\\+994|0)(10|50|51|55|60|70|77|99)\\d{7}$", message = "Invalid mobile number format.")
        @JsonProperty("mobile")
        String mobile
) {}
