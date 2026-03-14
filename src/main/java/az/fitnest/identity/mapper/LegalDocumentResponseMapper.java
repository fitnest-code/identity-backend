package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.LegalDocumentResponse;
import az.fitnest.identity.model.entity.LegalDocument;
import az.fitnest.identity.model.enums.LegalDocumentType;
import org.springframework.stereotype.Component;

@Component
public class LegalDocumentResponseMapper {
    public LegalDocumentResponse toResponse(LegalDocument doc, LegalDocumentType type) {
        return new LegalDocumentResponse(
            doc.getVersion(),
            type.name(),
            doc.getContent(),
            doc.getLastModifiedDate()
        );
    }
}
