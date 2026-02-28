package az.fitnest.identity.model.entity;
import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.constants.SocialProvider;
import az.fitnest.identity.model.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "social_auths",
        indexes = {
                @Index(name = "idx_social_auths_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_provider_provider_id",
                        columnNames = {"provider", "provider_id"}
                )
        }
)
@Getter
@Setter
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
}