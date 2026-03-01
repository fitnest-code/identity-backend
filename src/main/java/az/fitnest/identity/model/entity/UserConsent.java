package az.fitnest.identity.model.entity;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_consents", indexes = {
        @Index(name = "idx_user_consents_user_id", columnList = "user_id"),
        @Index(name = "idx_user_consents_user_accepted", columnList = "user_id, accepted_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent extends BaseAuditableEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "privacy_policy_version", nullable = false)
    private String privacyPolicyVersion;

    @Column(name = "terms_of_use_version", nullable = false)
    private String termsOfUseVersion;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "platform")
    private String platform;
}
