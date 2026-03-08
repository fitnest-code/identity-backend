package az.fitnest.identity.util;

import az.fitnest.identity.model.enums.UserStatus;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;

public class UserContext {

    public static Long getCurrentUserId() {
        return getPrincipalAsLong().orElse(null);
    }

    public static Long getRequiredUserId() {
        return getPrincipalAsLong()
                .orElseThrow(() -> new az.fitnest.identity.exception.UnauthorizedException("error.auth.unauthorized"));
    }

    public static String getCurrentUserEmail() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> (String) auth.getDetails())
                .orElse(null);
    }

    public static boolean hasRole(String role) {
        String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        return auth.getAuthorities().contains(new SimpleGrantedAuthority(roleName));
    }

    private static Optional<Long> getPrincipalAsLong() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return Optional.empty();
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) {
            return Optional.of(id);
        }

        if (principal instanceof String str) {
            try {
                return Optional.of(Long.parseLong(str));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
