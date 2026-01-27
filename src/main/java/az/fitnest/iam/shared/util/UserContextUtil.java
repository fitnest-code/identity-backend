package az.fitnest.iam.shared.util;

import az.fitnest.iam.shared.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserContextUtil {

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }

        if (auth != null && auth.getPrincipal() instanceof Integer userId) {
            return userId.longValue();
        }

        throw new UnauthorizedException("User not authenticated");
    }
}
