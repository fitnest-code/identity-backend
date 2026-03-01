package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.service.RedisKeyBuilder;
import az.fitnest.identity.service.*;
import az.fitnest.identity.service.*;

import az.fitnest.identity.model.entity.OtpSessionPayload;
import az.fitnest.identity.model.enums.OtpPurpose;
import az.fitnest.identity.model.enums.OtpVerificationStatus;
import az.fitnest.identity.exception.BadRequestException;
import az.fitnest.identity.exception.InternalServerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OtpStore {

    private static final String SAVE_OTP_SESSION_SCRIPT_STRING =
            "local active_pointer_key = KEYS[1] " +
                    "local new_session_key = KEYS[2] " +
                    "local session_key_prefix = ARGV[1] " +
                    "local session_json = ARGV[2] " +
                    "local ttl_seconds = tonumber(ARGV[3]) " +
                    "local new_session_id = ARGV[4] " +
                    "local old_session_id = redis.call('GET', active_pointer_key) " +
                    "if old_session_id and old_session_id ~= '' and old_session_id ~= new_session_id then " +
                    "    local old_session_key = session_key_prefix .. old_session_id " +
                    "    redis.call('DEL', old_session_key) " +
                    "end " +
                    "redis.call('SETEX', new_session_key, ttl_seconds, session_json) " +
                    "redis.call('SETEX', active_pointer_key, ttl_seconds, new_session_id) " +
                    "return 1";
    private static final String VERIFY_OTP_SCRIPT_STRING =
            "local session_key = KEYS[1] " +
                    "local max_attempts = tonumber(ARGV[1]) " +
                    "local is_valid = tonumber(ARGV[2]) " +
                    "local session_json = redis.call('GET', session_key) " +
                    "if not session_json then " +
                    "    return {0, '', ''} " +
                    "end " +
                    "local session = cjson.decode(session_json) " +
                    "if session.locked == true then " +
                    "    return {1, session_json, 'LOCKED'} " +
                    "end " +
                    "if session.verified == true then " +
                    "    return {1, session_json, 'ALREADY_VERIFIED'} " +
                    "end " +
                    "if is_valid == 0 then " +
                    "    local attempts = (session.attempts or 0) + 1 " +
                    "    session.attempts = attempts " +
                    "    if attempts >= max_attempts then " +
                    "        session.locked = true " +
                    "    end " +
                    "end " +
                    "if is_valid == 1 then " +
                    "    session.verified = true " +
                    "end " +
                    "local updated_json = cjson.encode(session) " +
                    "local ttl = redis.call('TTL', session_key) " +
                    "if ttl <= 0 then " +
                    "    return {1, session_json, 'EXPIRED'} " +
                    "end " +
                    "redis.call('SETEX', session_key, ttl, updated_json) " +
                    "return {1, updated_json, 'SUCCESS'}";
    private static final DefaultRedisScript<Long> SAVE_OTP_SESSION_SCRIPT = new DefaultRedisScript<>(
            SAVE_OTP_SESSION_SCRIPT_STRING,
            Long.class
    );
    private static final DefaultRedisScript<List> VERIFY_OTP_SCRIPT = new DefaultRedisScript<>(
            VERIFY_OTP_SCRIPT_STRING,
            List.class
    );
    private final RedisKeyBuilder redisKeyBuilder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveOtpSession(String sessionId, OtpSessionPayload payload, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(
                    redisKeyBuilder.sessionKey(sessionId),
                    json,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException e) {
            throw new InternalServerException("Failed to serialize OTP session");
        }
    }

    public Optional<OtpSessionPayload> getOtpSession(String sessionId) {
        String json = redisTemplate.opsForValue().get(
                redisKeyBuilder.sessionKey(sessionId)
        );

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, OtpSessionPayload.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public long getOtpSessionTtlSeconds(String sessionId) {
        long sec = redisTemplate.getExpire(
                redisKeyBuilder.sessionKey(sessionId),
                TimeUnit.SECONDS
        );

        if (sec < 0) {
            return 0L;
        }

        return sec;
    }

    public void updateOtpSession(String sessionId, OtpSessionPayload payload) {
        long ttlSeconds = getOtpSessionTtlSeconds(sessionId);

        if (ttlSeconds <= 0) {
            throw new BadRequestException("OTP session not found or expired");
        }

        saveOtpSession(sessionId, payload, ttlSeconds);
    }

    public void setActiveSessionPointer(OtpPurpose purpose, String email, String sessionId, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                redisKeyBuilder.activeSessionKey(purpose, email),
                sessionId,
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    public Optional<String> getActiveSessionPointer(OtpPurpose purpose, String email) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(
                        redisKeyBuilder.activeSessionKey(purpose, email)
                )
        );
    }

    public void deleteActivePointer(OtpPurpose purpose, String email) {
        redisTemplate.delete(redisKeyBuilder.activeSessionKey(purpose, email));
    }

    public void deleteSession(String sessionId) {
        redisTemplate.delete(redisKeyBuilder.sessionKey(sessionId));
    }

    public void saveOtpSessionAtomically(OtpPurpose purpose, String email, String sessionId,
                                         OtpSessionPayload payload, long ttlSeconds) {
        String activePointerKey = redisKeyBuilder.activeSessionKey(purpose, email);
        String sessionKey = redisKeyBuilder.sessionKey(sessionId);
        String sessionKeyPrefix = redisKeyBuilder.getSessionKeyPrefix();

        try {
            String sessionJson = objectMapper.writeValueAsString(payload);

            List<String> keys = Arrays.asList(activePointerKey, sessionKey);
            List<String> args = Arrays.asList(sessionKeyPrefix, sessionJson, String.valueOf(ttlSeconds), sessionId);

            try {
                redisTemplate.execute(SAVE_OTP_SESSION_SCRIPT, keys, args.toArray());
            } catch (Exception e) {
                throw new InternalServerException("Failed to execute Redis script for saving OTP session: " + e.getMessage());
            }
        } catch (JsonProcessingException e) {
            throw new InternalServerException("Failed to serialize OTP session");
        }
    }

    public Optional<OtpSessionPayload> getSessionForVerification(String sessionId) {
        return getOtpSession(sessionId);
    }

    public VerifyOtpResult verifyOtpAndUpdate(String sessionId, int maxAttempts, boolean isValid) {
        String sessionKey = redisKeyBuilder.sessionKey(sessionId);

        List<String> keys = Arrays.asList(sessionKey);
        List<String> args = Arrays.asList(String.valueOf(maxAttempts), isValid ? "1" : "0");

        List<?> result;
        try {
            result = redisTemplate.execute(VERIFY_OTP_SCRIPT, keys, args.toArray());
        } catch (Exception e) {
            throw new InternalServerException("Failed to execute Redis script for verifying OTP: " + e.getMessage());
        }

        return parseVerifyOtpResult(result);
    }

    private VerifyOtpResult parseVerifyOtpResult(Object result) {
        if (result instanceof List<?> list && list.size() >= 3) {
            long found = convertToLong(list.get(0));
            if (found == 0) {
                return VerifyOtpResult.notFound();
            }

            String sessionJson = extractString(list.get(1));
            String statusString = extractString(list.get(2));
            OtpVerificationStatus status = parseStatus(statusString);

            try {
                OtpSessionPayload payload = objectMapper.readValue(sessionJson, OtpSessionPayload.class);
                return new VerifyOtpResult(true, payload, status);
            } catch (JsonProcessingException e) {
                return VerifyOtpResult.notFound();
            }
        }

        return VerifyOtpResult.notFound();
    }

    private OtpVerificationStatus parseStatus(String statusString) {
        if (statusString == null || statusString.isEmpty()) {
            return OtpVerificationStatus.NOT_FOUND;
        }
        try {
            return OtpVerificationStatus.valueOf(statusString);
        } catch (IllegalArgumentException e) {
            return OtpVerificationStatus.NOT_FOUND;
        }
    }

    private String extractString(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value.toString();
    }

    private long convertToLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof byte[]) {
            try {
                return Long.parseLong(new String((byte[]) value));
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static class VerifyOtpResult {
        private final boolean found;
        private final OtpSessionPayload session;
        private final OtpVerificationStatus status;

        private VerifyOtpResult(boolean found, OtpSessionPayload session, OtpVerificationStatus status) {
            this.found = found;
            this.session = session;
            this.status = status;
        }

        public static VerifyOtpResult notFound() {
            return new VerifyOtpResult(false, null, OtpVerificationStatus.NOT_FOUND);
        }

        public boolean isFound() {
            return found;
        }

        public OtpSessionPayload getSession() {
            return session;
        }

        public OtpVerificationStatus getStatus() {
            return status;
        }

        public boolean isLocked() {
            return status == OtpVerificationStatus.LOCKED;
        }

        public boolean isAlreadyVerified() {
            return status == OtpVerificationStatus.ALREADY_VERIFIED;
        }

        public boolean isExpired() {
            return status == OtpVerificationStatus.EXPIRED;
        }

        public boolean isSuccess() {
            return status == OtpVerificationStatus.SUCCESS;
        }
    }

}
