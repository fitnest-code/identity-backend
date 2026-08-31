package az.fitnest.identity.controller;

import az.fitnest.identity.dto.request.EnvSyncUpsertStaffRequest;
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
 * Cross-environment staff provisioning (e.g. prod admin → create matching user in development).
 * Secured by shared {@code ENV_SYNC_SECRET}, not end-user JWTs.
 */
@RestController
@RequestMapping("/api/v1/internal/env-sync")
@RequiredArgsConstructor
@Hidden
public class EnvSyncController {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_FITNEST_STAFF"
    );

    private final UserService userService;

    @Value("${app.env-sync.secret:}")
    private String envSyncSecret;

    @PostMapping("/upsert-staff")
    public ResponseEntity<Map<String, Object>> upsertStaff(
            @RequestHeader(value = "X-Env-Sync-Secret", required = false) String secret,
            @RequestBody @Valid EnvSyncUpsertStaffRequest request) {

        if (!StringUtils.hasText(envSyncSecret) || !envSyncSecret.equals(secret)) {
            throw new ForbiddenException("Invalid env sync secret", "ENV_SYNC_FORBIDDEN");
        }

        String role = normalizeRole(request.role());
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ValidationException("error.validation", "INVALID_ROLE");
        }

        String mobile = MobileNumberUtils.normalize(request.mobile());
        if (!StringUtils.hasText(mobile)) {
            throw new ValidationException("error.validation", "INVALID_MOBILE");
        }

        User user = userService.upsertStaffForEnvSync(
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
