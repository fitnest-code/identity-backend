package az.fitnest.iam.auth.domain.model;

import az.fitnest.iam.auth.domain.enums.SocialProvider;
import az.fitnest.iam.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "social_auths",
        indexes = {
                @Index(name = "idx_social_auths_user_id", columnList = "user_id"),
                @Index(name = "idx_social_auths_provider_email", columnList = "provider,email")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"providerId"})
public class SocialAuth extends BaseAuditableEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 16)
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false, length = 256)
    private String providerId;

    @Column(name = "email", length = 320)
    private String email;
}