package az.fitnest.iam.shared.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Optional;

/**
 * Type-safe utility for accessing current user context from Spring Security.
 */
@Slf4j
public class UserContext {

    public static Long getCurrentUserId() {
        return getPrincipalAsLong().orElse(null);
    }

    public static Long getRequiredUserId() {
        return getPrincipalAsLong()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
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
        if (principal instanceof Long) {
            return (Optional<Long>) Optional.of((Long) principal);
        }
        
        if (principal instanceof String) {
            try {
                return Optional.of(Long.parseLong((String) principal));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
