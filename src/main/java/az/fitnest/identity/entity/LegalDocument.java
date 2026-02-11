package az.fitnest.identity.entity;

import az.fitnest.identity.constants.LegalDocumentType;
import az.fitnest.identity.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalDocument extends BaseAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LegalDocumentType type;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "language", nullable = false, length = 2)
    private String language;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}
