package az.fitnest.identity.dto;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.constants.OtpPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending OTP codes.
 * Used to initiate OTP verification for various purposes like registration or login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to send an OTP code to a mobile number")
public class OtpSendRequest {

    @NotNull
    @Schema(description = "Purpose of the OTP (REGISTRATION, LOGIN, PASSWORD_RESET)", example = "REGISTRATION", requiredMode = Schema.RequiredMode.REQUIRED)
    private OtpPurpose purpose;

    @NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "^(0|\\+994)(50|51|10|55|99|70|77|60)\\d{7}$", message = "Invalid mobile number format. Must be in Azerbaijan format (e.g., 0501234567 or +994501234567).")
    @Schema(description = "Mobile number to receive the OTP", example = "0501234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String mobile;
}