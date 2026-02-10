package az.fitnest.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FitnestSecurityFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // DEBUG: Log all headers to debug 403 issue
        if (request.getRequestURI().startsWith("/api/v1/internal")) {
            log.info("DEBUG: Incoming request to {}", request.getRequestURI());
            java.util.Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    log.info("DEBUG: Header {}: {}", headerName, request.getHeader(headerName));
                }
            }
        }

        // Ensure clean context at start - REMOVED: redundant and potentially harmful
        // SecurityContextHolder.clearContext();

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Validate JWT directly (public or internal delegation)
            authenticateViaJwt(authHeader.substring(7));
        }

        if (request.getRequestURI().startsWith("/api/v1/internal")) {
            // Internal call - Also check for internal identity headers or mesh principal
            // This ensures ROLE_INTERNAL is granted even if a user JWT is present
            authenticateViaInternalHeaders(request);

            // Log final authentication state for debugging
            org.springframework.security.core.Authentication finalAuth = SecurityContextHolder.getContext().getAuthentication();
            if (finalAuth != null) {
                log.info("Final auth for internal endpoint: principal={}, authorities={}, authenticated={}",
                        finalAuth.getPrincipal(), finalAuth.getAuthorities(), finalAuth.isAuthenticated());
            } else {
                log.warn("No authentication set for internal endpoint: {}", request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateViaInternalHeaders(HttpServletRequest request) {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        
        // Check for valid user ID (not null, not blank, not literal "null" string)
        if (userIdStr != null && !userIdStr.isBlank() && !userIdStr.equalsIgnoreCase("null")) {
            log.info("Authenticating internal request via headers for user: {}", userIdStr);
            boolean success = authenticateGatewayUser(request);
            if (!success) {
                // Fallback to internal service authentication if user parsing fails
                log.warn("Failed to parse user from headers, falling back to internal service auth");
                authenticateInternalService();
            }
        } else {
            log.info("Internal service-to-service call detected on {} (no valid X-User-Id)", request.getRequestURI());
            authenticateInternalService();
        }
    }

    private void authenticateViaJwt(String token) {
        try {
            Long userId = jwtService.parseUserId(token);
            java.util.List<String> roles = jwtService.parseRoles(token);
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user {} via JWT with roles {}", userId, roles);
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
    }

    private void authenticateInternalService() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "INTERNAL_SERVICE", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.info("Authenticated as INTERNAL_SERVICE with ROLE_INTERNAL");
    }

    private boolean authenticateGatewayUser(HttpServletRequest request) {
        String userIdStr = request.getHeader(USER_ID_HEADER);
        String email = request.getHeader(USER_EMAIL_HEADER);
        String rolesStr = request.getHeader(USER_ROLES_HEADER);

        try {
            Long userId = Long.parseLong(userIdStr);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            // We still grant ROLE_INTERNAL for endpoints that might still check it
            authorities.add(new SimpleGrantedAuthority("ROLE_INTERNAL"));

            if (rolesStr != null && !rolesStr.isBlank()) {
                authorities.addAll(Arrays.stream(rolesStr.split(","))
                        .map(role -> role.trim().toUpperCase())
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            
            auth.setDetails(email);
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.info("Authenticated internal request for user {} with authorities: {}", userId, authorities);
            return true;
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID format in X-User-Id header: '{}' - error: {}", userIdStr, e.getMessage());
            return false;
        }
    }
}
