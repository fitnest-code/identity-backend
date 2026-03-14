package az.fitnest.identity.mapper;

import az.fitnest.identity.dto.LegalDocumentResponse;
import az.fitnest.identity.model.entity.LegalDocument;
import az.fitnest.identity.model.enums.LegalDocumentType;

public final class LegalDocumentResponseMapper {
    private LegalDocumentResponseMapper() {}
    public static LegalDocumentResponse toResponse(LegalDocument doc, LegalDocumentType type) {
        return new LegalDocumentResponse(
            doc.getVersion(),
            type.name(),
            doc.getContent(),
            doc.getLastModifiedDate()
        );
    }
}
