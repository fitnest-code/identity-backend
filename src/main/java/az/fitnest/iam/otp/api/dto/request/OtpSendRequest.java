package az.fitnest.iam.otp.api.dto.request;

import az.fitnest.iam.otp.domain.enums.OtpPurpose;
import jakarta.validation.constraints.Email;
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
public class OtpSendRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private OtpPurpose purpose;

    @jakarta.validation.constraints.Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$", message = "Invalid mobile number format. Must start with 050, 051, 010, 055, 099, 070, 077, or 060 and follow with 7 digits.")
    private String mobile;
}