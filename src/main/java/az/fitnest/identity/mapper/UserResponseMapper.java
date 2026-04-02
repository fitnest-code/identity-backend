package az.fitnest.identity.mapper;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.dto.request.*;
import az.fitnest.identity.dto.response.*;
import az.fitnest.identity.dto.response.UserResponse;
import az.fitnest.identity.model.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {
    public UserResponse toResponse(User user) {
        return toResponse(user, false);
    }

    public UserResponse toResponse(User user, boolean consentRequired) {
        String profileImageUrl = null;

        boolean accountLocked = user.getStatus() == UserStatus.LOCKED &&
            user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.Instant.now());

        return new UserResponse(
                user.getId(),
                null,
                null,
                user.getMobile(),
                null,
                user.hasAccount(),
                user.isSetupRequired(),
                profileImageUrl,
                user.getLanguage(),
                user.getStatus() != null ? user.getStatus().name() : null,
                accountLocked,
                user.getCreatedDate(),
                consentRequired,
                user.getRole() != null ? user.getRole().getName() : null
        );
    }
}
