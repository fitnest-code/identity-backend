package az.fitnest.identity.dto;

public record UpdateLegalDocumentRequest(
        String version,
        String language,
        String content
) {
}
