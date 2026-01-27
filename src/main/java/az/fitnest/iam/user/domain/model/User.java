package az.fitnest.iam.user.domain.model;

import az.fitnest.iam.shared.persistence.BaseAuditableEntity;
import az.fitnest.iam.user.domain.enums.Language;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_mobile", columnList = "mobile"),
                @Index(name = "idx_users_email", columnList = "email")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_mobile", columnNames = "mobile"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditableEntity {

    @Override
    @AttributeOverride(name = "id", column = @Column(name = "user_id"))
    public Long getId() {
        return super.getId();
    }

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    private Language language;

    @Column(name = "has_account", nullable = false)
    @Builder.Default
    private Boolean hasAccount = Boolean.FALSE;

    @Column(name = "setup_required", nullable = false)
    @Builder.Default
    private Boolean setupRequired = Boolean.TRUE;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = Boolean.FALSE;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = Boolean.FALSE;
}