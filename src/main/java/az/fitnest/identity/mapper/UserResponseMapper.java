package az.fitnest.identity.mapper;
import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.dto.*;

import az.fitnest.identity.dto.UserResponse;
import az.fitnest.identity.model.entity.User;

public final class UserResponseMapper {

    private UserResponseMapper() {}

    public static UserResponse toResponse(User user) {
        return toResponse(user, false);
    }

    public static UserResponse toResponse(User user, boolean consentRequired) {
        UserResponse response = new UserResponse();
        response.setUserId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setMobile(user.getMobile());
        response.setEmail(user.getEmail());
        response.setHasAccount(user.hasAccount());
        response.setSetupRequired(user.isSetupRequired());
        response.setProfileImageUrl(user.getProfileImageUrl());
        response.setLanguage(user.getLanguage());
        response.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
        response.setAccountLocked(user.isAccountLocked());
        response.setCreatedAt(user.getCreatedDate());
        response.setConsentRequired(consentRequired);
        return response;
    }
}
