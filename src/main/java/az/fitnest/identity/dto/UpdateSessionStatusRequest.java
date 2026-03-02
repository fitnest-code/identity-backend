package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.SessionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateSessionStatusRequest(
    @NotNull(message = "Session status must not be null")
    SessionStatus sessionStatus
) {}
