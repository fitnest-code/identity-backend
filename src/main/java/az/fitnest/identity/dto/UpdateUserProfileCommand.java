package az.fitnest.identity.dto;

public record UpdateUserProfileCommand(
    String firstName,
    String lastName,
    String email,
    String mobile
) {}
