package az.fitnest.identity.dto.request;

public record UpdateLegalDocumentRequest(
    String version,
    String language,
    String content
) {}
