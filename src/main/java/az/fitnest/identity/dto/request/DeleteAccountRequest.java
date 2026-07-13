package az.fitnest.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for account deletion")
public record DeleteAccountRequest(
        @Schema(description = "Whether a permanent hard delete is requested. If false, the account is deactivated and permanently deleted after 30 days.", example = "false")
        Boolean needHardDelete
) {
}
