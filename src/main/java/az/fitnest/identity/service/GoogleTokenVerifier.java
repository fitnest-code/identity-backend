package az.fitnest.identity.service;

import az.fitnest.identity.model.enums.UserStatus;

public interface GoogleTokenVerifier {

    GoogleTokenClaims verify(String idToken);

    record GoogleTokenClaims(
            String userId,
            String email,
            boolean emailVerified,
            String givenName,
            String familyName,
            String name,
            String picture
    ) {
    }
}
