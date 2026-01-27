package az.fitnest.iam.user.api.dto.mapper;

import az.fitnest.iam.user.api.dto.response.UserResponse;
import az.fitnest.iam.user.domain.model.User;

public final class UserResponseMapper {

    private UserResponseMapper() {}

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .userId(String.valueOf(user.getId()))
                .fullName(user.getFullName())
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
                .build();
    }
}
