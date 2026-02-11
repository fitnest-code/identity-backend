package az.fitnest.identity.dto;

import lombok.Builder;

@Builder
public record UpdateUserProfileCommand(
        String firstName,
        String lastName,
        String email

) {
}

