package az.fitnest.identity.service;

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

    @Value("${auth.apple.client-id:}")
    private String appleClientId;

    @Value("${auth.apple.team-id:}")
    private String appleTeamId;

    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public AppleTokenClaims verify(String identityToken) {
        if (appleClientId == null || appleClientId.isEmpty()) {
            throw new IllegalStateException("Apple client ID not configured");
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

            if (!APPLE_ISSUER.equals(issuer)) {
                throw new UnauthorizedException("Invalid Apple token: invalid issuer");
            }

            if (!appleClientId.equals(audience)) {
                throw new UnauthorizedException("Invalid Apple token: invalid audience");
            }

            if (expirationTime != null && expirationTime.before(new Date())) {
                throw new UnauthorizedException("Invalid Apple token: expired");
            }

            if (subject == null || subject.isEmpty()) {
                throw new UnauthorizedException("Invalid Apple token: missing subject");
            }

            String keyId = header.getKeyID();
            RSAPublicKey publicKey = getApplePublicKey(keyId);
            if (publicKey == null) {
                throw new UnauthorizedException("Invalid Apple token: public key not found");
            }

            if (!signedJWT.verify(new RSASSAVerifier(publicKey))) {
                throw new UnauthorizedException("Invalid Apple token: signature verification failed");
            }

            return new AppleTokenClaims(subject, email, emailVerified != null && emailVerified);
        } catch (ParseException | JOSEException e) {
            throw new UnauthorizedException("Invalid Apple token: " + e.getMessage());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid Apple token: " + e.getMessage());
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
            throw new UnauthorizedException("Failed to fetch Apple public keys: " + e.getMessage());
        }
    }

    public record AppleTokenClaims(String userId, String email, boolean emailVerified) {}
}
