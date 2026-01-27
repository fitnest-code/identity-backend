package az.fitnest.iam.auth.adapter.service;

import az.fitnest.iam.auth.api.dto.request.AppleSocialRequest;
import az.fitnest.iam.auth.api.dto.request.GoogleSocialRequest;
import az.fitnest.iam.auth.api.dto.response.LoginResponse;
import az.fitnest.iam.auth.adapter.persistence.SocialAuthRepository;
import az.fitnest.iam.auth.domain.enums.SocialProvider;
import az.fitnest.iam.auth.domain.model.SocialAuth;
import az.fitnest.iam.shared.exception.ConflictException;
import az.fitnest.iam.shared.exception.InvalidCredentialsException;
import az.fitnest.iam.user.adapter.persistence.UserRepository;
import az.fitnest.iam.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final UserRepository userRepository;
    private final SocialAuthRepository socialAuthRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier appleTokenVerifier;
    private final TokenIssuanceService tokenIssuanceService;

    @Transactional
    public LoginResponse socialLoginApple(AppleSocialRequest request) {
        AppleTokenVerifier.AppleTokenClaims claims = appleTokenVerifier.verify(request.getIdentityToken());
        String providerId = claims.userId();
        String email = claims.email();
        
        Optional<SocialAuth> existingSocialAuth = socialAuthRepository.findByProviderAndProviderId(
                SocialProvider.APPLE, providerId);
        
        if (existingSocialAuth.isPresent()) {
            User user = userRepository.findById(existingSocialAuth.get().getUserId())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
            return tokenIssuanceService.issueTokens(user);
        }
        
        if (email != null) {
            Optional<SocialAuth> existingByEmail = socialAuthRepository.findByProviderAndEmailIgnoreCase(
                    SocialProvider.APPLE, email);
            if (existingByEmail.isPresent()) {
                throw new ConflictException("Unable to complete social login with this account");
            }
            
            Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
            if (existingUser.isPresent()) {
                throw new ConflictException("Unable to complete social login with this account");
            }
        }
        
        User newUser = createUserForSocialLogin(
                email,
                request.getFullName() != null ? request.getFullName() : "User",
                null);
        
        SocialAuth socialAuth = SocialAuth.builder()
                .userId(newUser.getId())
                .provider(SocialProvider.APPLE)
                .providerId(providerId)
                .email(email)
                .build();
        socialAuthRepository.save(socialAuth);
        
        return tokenIssuanceService.issueTokens(newUser);
    }

    @Transactional
    public LoginResponse socialLoginGoogle(GoogleSocialRequest request) {
        GoogleTokenVerifier.GoogleTokenClaims claims = googleTokenVerifier.verify(request.getIdToken());
        String providerId = claims.userId();
        String email = claims.email();
        
        Optional<SocialAuth> existingSocialAuth = socialAuthRepository.findByProviderAndProviderId(
                SocialProvider.GOOGLE, providerId);
        
        if (existingSocialAuth.isPresent()) {
            User user = userRepository.findById(existingSocialAuth.get().getUserId())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
            return tokenIssuanceService.issueTokens(user);
        }
        
        if (email != null) {
            Optional<SocialAuth> existingByEmail = socialAuthRepository.findByProviderAndEmailIgnoreCase(
                    SocialProvider.GOOGLE, email);
            if (existingByEmail.isPresent()) {
                throw new ConflictException("Unable to complete social login with this account");
            }
            
            Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
            if (existingUser.isPresent()) {
                throw new ConflictException("Unable to complete social login with this account");
            }
        }
        
        User newUser = createUserForSocialLogin(
                email,
                request.getFullName() != null ? request.getFullName() : "User",
                null);
        
        SocialAuth socialAuth = SocialAuth.builder()
                .userId(newUser.getId())
                .provider(SocialProvider.GOOGLE)
                .providerId(providerId)
                .email(email)
                .build();
        socialAuthRepository.save(socialAuth);
        
        return tokenIssuanceService.issueTokens(newUser);
    }

    private User createUserForSocialLogin(String email, String fullName, String mobile) {
        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .mobile(mobile != null ? mobile : "")
                .passwordHash(null)
                .hasAccount(true)
                .setupRequired(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .isDeleted(false)
                .build();
        return userRepository.save(user);
    }
}
