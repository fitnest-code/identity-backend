package az.fitnest.iamservice.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSendResponse {
    private String otpSessionId;
    private Integer expiresInSeconds;
    private Integer resendAvailableInSeconds;
}
