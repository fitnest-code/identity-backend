package az.fitnest.identity.controller;

import az.fitnest.identity.dto.request.EnsureStaffAccessRequest;
import az.fitnest.identity.exception.ForbiddenException;
import az.fitnest.identity.exception.ValidationException;
import az.fitnest.identity.model.entity.User;
import az.fitnest.identity.service.UserService;
import az.fitnest.identity.util.MobileNumberUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * Ensures Fitnest staff can log into another environment with the same credentials.
 * Called cluster-internally (not via the public API gateway). Secured by shared secret.
 */
@RestController
@RequestMapping("/api/v1/internal/staff-access")
@RequiredArgsConstructor
@Hidden
public class StaffAccessController {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_FITNEST_STAFF"
    );

    private final UserService userService;

    @Value("${app.staff-access.secret:}")
    private String staffAccessSecret;

    @PostMapping("/ensure")
    public ResponseEntity<Map<String, Object>> ensureStaff(
            @RequestHeader(value = "X-Staff-Access-Secret", required = false) String secret,
            @RequestHeader(value = "X-Env-Sync-Secret", required = false) String legacySecret,
            @RequestBody @Valid EnsureStaffAccessRequest request) {

        String provided = StringUtils.hasText(secret) ? secret : legacySecret;
        if (!StringUtils.hasText(staffAccessSecret) || !staffAccessSecret.equals(provided)) {
            throw new ForbiddenException("Invalid staff access secret", "STAFF_ACCESS_FORBIDDEN");
        }

        String role = normalizeRole(request.role());
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ValidationException("error.validation", "INVALID_ROLE");
        }

        String mobile = MobileNumberUtils.normalize(request.mobile());
        if (!StringUtils.hasText(mobile)) {
            throw new ValidationException("error.validation", "INVALID_MOBILE");
        }

        User user = userService.ensureStaffAccess(
                mobile,
                request.password(),
                role,
                blankToNull(request.firstName()),
                blankToNull(request.lastName())
        );

        return ResponseEntity.ok(Map.of(
                "user_id", user.getId(),
                "mobile", user.getMobile(),
                "role", user.getRole() != null ? user.getRole().getName() : role
        ));
    }

    private static String normalizeRole(String role) {
        if (role == null) return "";
        String trimmed = role.trim();
        if (trimmed.isEmpty()) return "";
        return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
