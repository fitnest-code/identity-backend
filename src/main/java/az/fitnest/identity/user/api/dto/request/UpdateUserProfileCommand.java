package az.fitnest.identity.user.api.dto.request;

import lombok.Builder;

@Builder
public record UpdateUserProfileCommand(
        String firstName,
        String lastName,
        String email

) {
}

