package az.fitnest.iamservice.service;

import az.fitnest.iamservice.dto.common.OtpSessionPayload;
import az.fitnest.iamservice.enums.OtpPurpose;

import java.time.Duration;
import java.util.Optional;

public interface RedisOtpStoreService {

    boolean isCooldownActive(OtpPurpose purpose, String email);
    Duration getCooldownRemaining(OtpPurpose purpose, String email);
    void startCooldown(OtpPurpose purpose, String email, long cooldownSeconds);
    void saveOtpSession(String sessionId, OtpSessionPayload payload, long ttlSeconds);
    Optional<OtpSessionPayload> getOtpSession(String sessionId);
    void setActiveSessionPointer(OtpPurpose purpose, String email, String sessionId, long ttlSeconds);
    Optional<String> getActiveSessionPointer(OtpPurpose purpose, String email);
    void deleteSession(String sessionId);
}
