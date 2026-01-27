package az.fitnest.iam.user.application.command;

import lombok.Builder;

@Builder
public record UpdateUserProfileCommand(
        String firstName,
        String lastName,
        String email
) {
}

