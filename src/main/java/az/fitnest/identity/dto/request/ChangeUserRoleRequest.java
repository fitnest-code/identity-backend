package az.fitnest.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * @author: nijataghayev
 */

public record ChangeUserRoleRequest(
        @NotBlank String role
) {}
