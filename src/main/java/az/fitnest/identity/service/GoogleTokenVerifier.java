package az.fitnest.identity.service;

public interface GoogleTokenVerifier {

    GoogleTokenClaims verify(String idToken);

    record GoogleTokenClaims(String userId, String email, boolean emailVerified) {}
}