package az.fitnest.identity.model.entity;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.SessionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_mobile", columnNames = "mobile")
        }
)
@AttributeOverride(name = "id", column = @Column(name = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditableEntity {

    @Column(name = "mobile", length = 20)
    private String mobile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @JsonIgnore
    @ToString.Exclude
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "language")
    private String language;

    @Builder.Default
    @Column(name = "has_account", nullable = false)
    private boolean hasAccount = false;

    @Builder.Default
    @Column(name = "setup_required", nullable = false)
    private boolean setupRequired = true;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "session_status")
    private SessionStatus sessionStatus = SessionStatus.NO_SESSIONS;
    
    @Builder.Default
    @Column(name = "has_local_password", nullable = false, columnDefinition = "boolean default false")
    private boolean hasLocalPassword = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "inactive_at")
    private Instant inactiveAt;

    public String getMobile() { return mobile; }
    public UserStatus getStatus() { return status; }
    public SessionStatus getSessionStatus() { return sessionStatus; }
    public Role getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public String getLanguage() { return language; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }

    public boolean hasAccount() {
        return hasAccount;
    }

    public boolean isSetupRequired() {
        return setupRequired;
    }
}
