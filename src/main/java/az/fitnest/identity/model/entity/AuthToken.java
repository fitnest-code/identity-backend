package az.fitnest.identity.model.entity;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "auth_tokens",
        indexes = {
                @Index(name = "idx_auth_tokens_user_id", columnList = "user_id"),
                @Index(name = "idx_auth_tokens_access_token", columnList = "access_token", unique = true),
                @Index(name = "idx_auth_tokens_refresh_token", columnList = "refresh_token", unique = true),
                @Index(name = "idx_auth_tokens_jti", columnList = "jti", unique = true)
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"accessTokenHash", "refreshTokenHash"})
public class AuthToken extends BaseAuditableEntity {

    @Column(name = "access_token", nullable = false, length = 2000)
    private String accessTokenHash;

    @Column(name = "refresh_token", nullable = false, length = 2000)
    private String refreshTokenHash;

    @Column(name = "jti")
    private String jti;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "access_expires_at", nullable = false)
    private Instant accessExpiresAt;

    @Column(name = "refresh_expires_at")
    private Instant refreshExpiresAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;
}