package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppleTokenVerifier {

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${auth.apple.client-id:}")
    private String appleClientId;
    @Value("${auth.apple.team-id:}")
    private String appleTeamId;

    private final MessageSource messageSource;

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    public AppleTokenClaims verify(String identityToken) {
        if (appleClientId == null || appleClientId.isEmpty()) {
            throw new IllegalStateException(getMessage("error.service.external_provider_not_configured"));
        }
        try {
            SignedJWT signedJWT = SignedJWT.parse(identityToken);
            JWSHeader header = signedJWT.getHeader();
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            String issuer = claims.getIssuer();
            String audience = claims.getAudience().stream().findFirst().orElse(null);
            Date expirationTime = claims.getExpirationTime();
            String subject = claims.getSubject();
            String email = claims.getStringClaim("email");
            Boolean emailVerified = claims.getBooleanClaim("email_verified");
            if (!APPLE_ISSUER.equals(issuer) || !appleClientId.equals(audience) || subject == null || subject.isEmpty()) {
                throw new UnauthorizedException(getMessage("error.auth.external_token_invalid"));
            }
            if (expirationTime != null && expirationTime.before(new Date())) {
                throw new UnauthorizedException(getMessage("error.auth.external_token_expired"));
            }
            String keyId = header.getKeyID();
            RSAPublicKey publicKey = getApplePublicKey(keyId);
            if (publicKey == null || !signedJWT.verify(new RSASSAVerifier(publicKey))) {
                throw new UnauthorizedException(getMessage("error.auth.external_token_invalid"));
            }
            return new AppleTokenClaims(subject, email, emailVerified != null && emailVerified);
        } catch (ParseException | JOSEException e) {
            throw new UnauthorizedException(getMessage("error.auth.external_auth_failed"));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException(getMessage("error.service.external_auth_error"));
        }
    }

    private RSAPublicKey getApplePublicKey(String keyId) {
        try {
            URL jwksUrl = new URL(APPLE_JWKS_URL);
            try (InputStream inputStream = jwksUrl.openStream()) {
                JsonNode jwksJson = objectMapper.readTree(inputStream);
                JWKSet jwkSet = JWKSet.parse(jwksJson.toString());
                List<JWK> keys = jwkSet.getKeys();
                for (JWK key : keys) {
                    if (keyId != null && keyId.equals(key.getKeyID())) {
                        if (key instanceof RSAKey) {
                            return ((RSAKey) key).toRSAPublicKey();
                        }
                    }
                }
                if (keys.size() == 1 && keys.get(0) instanceof RSAKey) {
                    return ((RSAKey) keys.get(0)).toRSAPublicKey();
                }
                return null;
            }
        } catch (Exception e) {
            throw new UnauthorizedException(getMessage("error.service.external_auth_error"));
        }
    }

    public record AppleTokenClaims(String userId, String email, boolean emailVerified) {
    }
}
