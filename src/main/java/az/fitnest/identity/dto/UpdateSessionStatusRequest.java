package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.model.enums.SessionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSessionStatusRequest {
    @NotNull(message = "Session status must not be null")
    private SessionStatus sessionStatus;
}
