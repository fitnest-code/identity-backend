package az.fitnest.identity.security;

import az.fitnest.identity.model.enums.UserStatus;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${jwt.refresh-ttl-seconds}") long refreshTtlSeconds
    ) {
        if (secret == null || secret.trim().length() < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 32 characters (HS256 requirement).");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String generateAccessToken(Long userId, java.util.Collection<String> roles, String language) {
        return buildToken(userId, accessTtlSeconds, Map.of(
                "typ", "access",
                "roles", roles,
                "lang", language != null ? language : "AZ"
        ));
    }

    public String generateRefreshToken(Long userId) {
        return buildToken(userId, refreshTtlSeconds, Map.of("typ", "refresh"));
    }

    public Long parseUserId(String token, String expectedType) {
        Claims claims = parseClaims(token);

        String actualType = claims.get("typ", String.class);
        if (expectedType != null && !expectedType.equals(actualType)) {
            throw new IllegalArgumentException("Invalid JWT token type. Expected: " + expectedType + ", Got: " + actualType);
        }

        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new IllegalArgumentException("JWT subject (sub) is missing.");
        }
        try {
            return Long.parseLong(sub);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("JWT subject (sub) is not a valid Long.");
        }
    }

    public Long parseUserId(String token) {
        return parseUserId(token, null);
    }

    public Instant parseExpiration(String token) {
        Claims claims = parseClaims(token);
        Date exp = claims.getExpiration();
        if (exp == null) {
            throw new IllegalArgumentException("JWT expiration (exp) is missing.");
        }
        return exp.toInstant();
    }

    @SuppressWarnings("unchecked")
    public java.util.List<String> parseRoles(String token) {
        Claims claims = parseClaims(token);
        Object roles = claims.get("roles");
        if (roles instanceof java.util.List<?>) {
            return (java.util.List<String>) roles;
        }
        return java.util.Collections.emptyList();
    }

    public String parseJti(String token) {
        return parseClaims(token).getId();
    }

    private String buildToken(Long userId, long ttlSeconds, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        String jti = UUID.randomUUID().toString().replace("-", "");

        return Jwts.builder()
                .claims(extraClaims)
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .id(jti)
                .signWith(key)
                .compact();
    }

    private Claims parseClaims(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);

        return jws.getPayload();
    }
}
