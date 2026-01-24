package az.fitnest.iamservice.dto.request;

import az.fitnest.iamservice.enums.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendRequest {
    @NotBlank
    @Email
    private String email;

    @NotNull
    private OtpPurpose purpose;
}
