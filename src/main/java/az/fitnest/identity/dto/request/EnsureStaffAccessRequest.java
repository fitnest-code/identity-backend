package az.fitnest.identity.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EnsureStaffAccessRequest(
        @NotBlank String mobile,
        @NotBlank String password,
        @NotBlank String role,
        String firstName,
        String lastName
) {
}
