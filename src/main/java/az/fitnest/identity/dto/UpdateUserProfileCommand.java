package az.fitnest.identity.dto;

import az.fitnest.identity.model.enums.UserStatus;

import lombok.Builder;

@Builder
public record UpdateUserProfileCommand(
        String firstName,
        String lastName,
        String email,
        String mobile
) {
}

