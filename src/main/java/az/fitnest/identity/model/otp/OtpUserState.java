package az.fitnest.identity.model.otp;

import java.io.Serializable;
import java.time.Instant;

public class OtpUserState implements Serializable {
    private Long userId;
    private Integer resendCount;
    private Instant lastSentAt;
    private Integer dailySendCount;
    private Instant lockedUntil;
    private String key;

    public OtpUserState() {
    }

    public OtpUserState(Long userId) {
        this.userId = userId;
        this.resendCount = 0;
        this.dailySendCount = 0;
    }

    public OtpUserState(String key) {
        this.key = key;
        this.resendCount = 0;
        this.dailySendCount = 0;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getResendCount() {
        return resendCount;
    }

    public void setResendCount(Integer resendCount) {
        this.resendCount = resendCount;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(Instant lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    public Integer getDailySendCount() {
        return dailySendCount;
    }

    public void setDailySendCount(Integer dailySendCount) {
        this.dailySendCount = dailySendCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
