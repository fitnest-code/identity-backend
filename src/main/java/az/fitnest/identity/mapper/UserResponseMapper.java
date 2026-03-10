package az.fitnest.identity.mapper;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.dto.*;

import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.model.entity.User;

public final class UserResponseMapper {

    private UserResponseMapper() {
    }

    public static UserResponse toResponse(User user) {
        return toResponse(user, false);
    }

    public static UserResponse toResponse(User user, boolean consentRequired) {
        String profileImageUrl = user.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.isBlank() && !profileImageUrl.startsWith("http")) {
            profileImageUrl = "/api/v1/me/profile/images/" + profileImageUrl;
        }

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getMobile(),
                user.getEmail(),
                user.hasAccount(),
                user.isSetupRequired(),
                profileImageUrl,
                user.getLanguage(),
                user.getStatus() != null ? user.getStatus().name() : null,
                user.isAccountLocked(),
                user.getCreatedDate(),
                consentRequired,
                user.getRole() != null ? user.getRole().getName() : null
        );
    }
}
