package az.fitnest.iam.user.domain.model;

import az.fitnest.iam.shared.persistence.BaseAuditableEntity;
import az.fitnest.iam.user.domain.enums.Language;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.Objects;


@Entity
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE user_id = ?")
@Where(clause = "is_deleted = false")
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



    @JsonIgnore
    @ToString.Exclude
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private Language language;

    @Builder.Default
    @Column(name = "has_account", nullable = false)
    private boolean hasAccount = false;

    @Builder.Default
    @Column(name = "setup_required", nullable = false)
    private boolean setupRequired = true;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Builder.Default
    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public boolean hasAccount() { return hasAccount; }
    public boolean isSetupRequired() { return setupRequired; }
    public boolean isAccountLocked() { return accountLocked || (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())); }
    public boolean isDeleted() { return isDeleted; }


}