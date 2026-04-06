package az.fitnest.identity.mapper;

import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.dto.request.*;
import az.fitnest.identity.dto.response.*;
import az.fitnest.identity.dto.response.UserResponse;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.service.LegalService;
import az.fitnest.identity.service.UserProfileGrpcClient;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserResponseMapper {

    private final UserProfileGrpcClient userProfileGrpcClient;
    private final LegalService legalService;

    public UserResponse toResponse(User user) {
        return toResponse(user, legalService.isConsentRequired(user.getId()));
    }

    public UserResponse toResponse(User user, boolean consentRequired) {
        String profileImageUrl = null;
        String firstName = null;
        String lastName = null;
        String email = null;

        try {
            var profile = userProfileGrpcClient.getUserProfileDetails(user.getId());
            if (profile != null) {
                profileImageUrl = profile.getProfileImageUrl();
                firstName = profile.getFirstName();
                lastName = profile.getLastName();
                email = profile.getEmail();
            }
        } catch (Exception ignored) {}

        boolean accountLocked = user.getStatus() == UserStatus.LOCKED &&
            user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.Instant.now());

        return new UserResponse(
                user.getId(),
                firstName,
                lastName,
                user.getMobile(),
                email,
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
