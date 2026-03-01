package az.fitnest.identity.model.entity;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.SessionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Entity representing a user in the identity system.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_mobile", columnNames = "mobile"),
        }
)
@AttributeOverride(name = "id", column = @Column(name = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditableEntity {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    @Column(name = "email")
    private String email;

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

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "session_status")
    private SessionStatus sessionStatus = SessionStatus.NO_SESSIONS;

    public boolean hasAccount() {
        return hasAccount;
    }

    public boolean isSetupRequired() {
        return setupRequired;
    }

    public boolean isAccountLocked() {
        return status == UserStatus.LOCKED && lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public boolean isDeleted() {
        return this.status == UserStatus.INACTIVE;
    }
}