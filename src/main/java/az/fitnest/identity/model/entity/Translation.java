package az.fitnest.identity.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "translations", indexes = {
        @Index(name = "idx_translations_entity", columnList = "entity_type, entity_id, language_code, field_name")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_translations_entity_field_lang", columnNames = {"entity_type", "entity_id", "field_name", "language_code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Translation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;
}
