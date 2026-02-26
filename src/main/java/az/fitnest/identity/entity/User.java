package az.fitnest.identity.entity;

import az.fitnest.identity.entity.BaseAuditableEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Entity representing a user in the identity system.
 * Manages user authentication, authorization, and account status.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Soft deletion by using a status field (ACTIVE / INACTIVE) instead of physical removal</li>
 *   <li>Account locking mechanism to prevent brute-force attacks</li>
 *   <li>Multi-language support</li>
 *   <li>Profile setup tracking</li>
 * </ul>
 *
 * <p>Security Features:
 * <ul>
 *   <li>Failed login attempt tracking</li>
 *   <li>Temporary account locking after multiple failed attempts</li>
 *   <li>Password hash storage (never plain text)</li>
 * </ul>
 *
 * @see Role
 * @see Language
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

    public enum Status {
        ACTIVE,
        INACTIVE,
        LOCKED,
        NO_SESSIONS
    }

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
    private Status status = Status.ACTIVE;

    public boolean hasAccount() { return hasAccount; }
    public boolean isSetupRequired() { return setupRequired; }
    public boolean isAccountLocked() {
        return status == Status.LOCKED && lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public boolean isDeleted() { return this.status == Status.INACTIVE; }

}