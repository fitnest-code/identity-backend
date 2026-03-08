package az.fitnest.identity.service.impl;

import az.fitnest.identity.model.enums.UserStatus;

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

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    @Value("${auth.google.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    private GoogleIdTokenVerifier getVerifier() {
        if (verifier == null) {
            verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
        }
        return verifier;
    }

    @Override
    public GoogleTokenClaims verify(String idToken) {
        if (googleClientId == null || googleClientId.isEmpty()) {
            throw new IllegalStateException("Google client ID not configured");
        }

        try {
            GoogleIdToken token = getVerifier().verify(idToken);
            if (token == null) {
                throw new UnauthorizedException("error.service.external_auth_error");
            }

            GoogleIdToken.Payload payload = token.getPayload();

            String userId = payload.getSubject();
            String email = (String) payload.get("email");
            String emailVerified = String.valueOf(payload.get("email_verified"));
            String issuer = payload.getIssuer();
            Long expirationTimeSeconds = payload.getExpirationTimeSeconds();

            if (userId == null || userId.isEmpty()) {
                throw new UnauthorizedException("error.service.external_auth_error");
            }

            if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
                throw new UnauthorizedException("error.service.external_auth_error");
            }

            if (expirationTimeSeconds != null && expirationTimeSeconds * 1000L < System.currentTimeMillis()) {
                throw new UnauthorizedException("error.service.external_auth_error");
            }

            return new GoogleTokenClaims(userId, email, "true".equalsIgnoreCase(emailVerified));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("error.service.external_auth_error");
        }
    }
}
