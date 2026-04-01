package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.exception.InvalidCredentialsException;
import az.fitnest.identity.exception.UnauthorizedException;
import az.fitnest.identity.service.GoogleTokenVerifier;
import az.fitnest.identity.service.GoogleTokenVerifier.GoogleTokenClaims;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    @Value("${auth.google.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierImpl.class);

    @PostConstruct
    private void initVerifier() {
        log.info("Initializing Google ID Token Verifier with Client ID: {}", googleClientId);
        verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @Override
    public GoogleTokenClaims verify(String idToken) {
        if (googleClientId == null || googleClientId.isEmpty()) {
            throw new IllegalStateException("Google client ID not configured");
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                try {
                    GoogleIdToken unverifiedToken = GoogleIdToken.parse(GsonFactory.getDefaultInstance(), idToken);
                    GoogleIdToken.Payload payload = unverifiedToken.getPayload();
                    log.error("Google token verification failed. Token payload: aud={}, azp={}, iss={}, exp={}",
                        payload.getAudience(), payload.get("azp"), payload.getIssuer(), payload.getExpirationTimeSeconds());
                } catch (Exception parseEx) {
                    log.error("Google token verification failed and could not parse token: {}", parseEx.getMessage());
                }
                throw new UnauthorizedException("error.service.external_auth_error");
            }

            GoogleIdToken.Payload payload = token.getPayload();

            String userId = payload.getSubject();
            String email = (String) payload.get("email");
            if (email == null || email.isEmpty()) {
                throw new UnauthorizedException("Google account does not provide an email address");
            }

            Object emailVerifiedObj = payload.get("email_verified");
            boolean emailVerified = false;
            if (emailVerifiedObj instanceof Boolean b) {
                emailVerified = b;
            } else if (emailVerifiedObj instanceof String s) {
                emailVerified = Boolean.parseBoolean(s);
            }

            if (!emailVerified) {
                throw new InvalidCredentialsException("Google account email is not verified");
            }

            String givenName = (String) payload.get("given_name");
            String familyName = (String) payload.get("family_name");
            String name = (String) payload.get("name");
            String issuer = payload.getIssuer();

            Object audience = payload.getAudience();
            boolean audMatch = false;
            if (audience instanceof String s) {
                audMatch = googleClientId.equals(s);
            } else if (audience instanceof java.util.List<?> list) {
                audMatch = list.contains(googleClientId);
            }

            if (!audMatch) {
                throw new UnauthorizedException("Google ID token audience does not match configured client ID");
            }

            if (userId == null || userId.isEmpty()) {
                throw new UnauthorizedException("error.service.external_auth_error");
            }

            if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
                throw new UnauthorizedException("error.service.external_auth_error");
            }

            return new GoogleTokenClaims(
                userId,
                email,
                true,
                givenName,
                familyName,
                name
            );
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification failed", e);
            throw new UnauthorizedException("error.service.external_auth_error");
        }
    }
}
