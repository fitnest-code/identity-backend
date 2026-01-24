package az.fitnest.iamservice.util.helper;

import az.fitnest.iamservice.enums.OtpPurpose;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyHelper {

    public String sessionKey(String sessionId) {
        return "otp:session:" + sessionId;
    }

    public String cooldownKey(OtpPurpose purpose, String email) {
        return "otp:cooldown:" + purpose + ":" + email;
    }

    public String activeSessionKey(OtpPurpose purpose, String email) {
        return "otp:active:" + purpose + ":" + email;
    }
}