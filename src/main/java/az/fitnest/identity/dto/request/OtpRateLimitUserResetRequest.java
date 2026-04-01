package az.fitnest.identity.dto.request;

import lombok.Data;

@Data
public class OtpRateLimitUserResetRequest {
    private Long userId;
}
