package az.fitnest.iam.otp.adapter.store.redis;

import az.fitnest.iam.otp.domain.enums.OtpPurpose;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyBuilder {

    public String sessionKey(String sessionId) {
        return "otp:session:" + sessionId;
    }

    public String cooldownKey(OtpPurpose purpose, String email) {
        return "otp:cooldown:" + purpose.name() + ":" + email;
    }

    public String activeSessionKey(OtpPurpose purpose, String email) {
        return "otp:active:" + purpose.name() + ":" + email;
    }
}
