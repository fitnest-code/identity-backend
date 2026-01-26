package az.fitnest.iam.auth.domain.model;

import az.fitnest.iam.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "auth_tokens",
        indexes = {
                @Index(name = "idx_auth_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_auth_tokens_access_token", columnList = "access_token"),
                @Index(name = "idx_auth_tokens_refresh_token", columnList = "refresh_token")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"accessToken", "refreshToken"})
public class AuthToken extends BaseAuditableEntity {

    @Column(name = "access_token", nullable = false, length = 2000)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, length = 2000)
    private String refreshToken;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "access_expires_at", nullable = false)
    private LocalDateTime accessExpiresAt;

    @Column(name = "refresh_expires_at")
    private LocalDateTime refreshExpiresAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;
}