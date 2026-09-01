package az.fitnest.identity.service;

import az.fitnest.identity.exception.ConflictException;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.model.enums.UserStatus;
import az.fitnest.identity.repository.SocialAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Rules for linking Apple/Google sign-in to an existing mobile-registered account.
 */
@Service
@RequiredArgsConstructor
public class SocialPhoneLinkService {

    private final SocialAuthRepository socialAuthRepository;

    /**
     * OTP on the mobile proves ownership. Merge is allowed when the existing account is active,
     * phone-based, and has no social provider linked yet (even if profile email is set).
     */
    public boolean canMergeSocialIntoExistingPhoneUser(User existingUser) {
        if (existingUser == null) {
            return false;
        }
        UserStatus status = existingUser.getStatus();
        if (status == UserStatus.INACTIVE
                || status == UserStatus.DELETED
                || status == UserStatus.BLOCKED) {
            return false;
        }
        return socialAuthRepository.findByUserId(existingUser.getId()).isEmpty();
    }

    public void assertCanMergeSocialIntoExistingPhoneUser(User existingUser) {
        if (!canMergeSocialIntoExistingPhoneUser(existingUser)) {
            throw new ConflictException("error.service.mobile_already_in_use");
        }
    }
}
