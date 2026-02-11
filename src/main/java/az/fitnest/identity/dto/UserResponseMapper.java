package az.fitnest.identity.dto;

import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.entity.User;

public final class UserResponseMapper {

    private UserResponseMapper() {}

    public static UserResponse toResponse(User user) {
        return toResponse(user, false);
    }

    public static UserResponse toResponse(User user, boolean consentRequired) {
        return UserResponse.builder()
                .userId(String.valueOf(user.getId()))
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mobile(user.getMobile())
                .email(user.getEmail())

                .hasAccount(user.hasAccount())
                .setupRequired(user.isSetupRequired())
                .profileImageUrl(user.getProfileImageUrl())
                .language(user.getLanguage() != null ? user.getLanguage().name() : null)
                .accountLocked(user.isAccountLocked())
                .createdAt(user.getCreatedDate())
                .consentRequired(consentRequired)
                .build();
    }
}
