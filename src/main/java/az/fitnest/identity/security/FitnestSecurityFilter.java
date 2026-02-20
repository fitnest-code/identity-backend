package az.fitnest.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FitnestSecurityFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    
    // Pre-compiled set of paths that don't need security filter processing
    private static final Set<String> SKIP_FILTER_PATH_PREFIXES = Set.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator",
            "/webjars"
    );

    private final JwtService jwtService;

    /**
     * Skip filter processing for public endpoints like Swagger and Actuator.
     * This improves performance by avoiding unnecessary JWT parsing.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_FILTER_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Ensure clean context at start - REMOVED: redundant and potentially harmful
        // SecurityContextHolder.clearContext();

        String gatewayFlag = request.getHeader("X-From-Gateway");
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String requestId = request.getHeader("X-Request-Id");
        String caller = request.getHeader("X-Service-Name");

        // Prefer Pattern A headers if from Gateway
        if ("1".equals(gatewayFlag) && userIdHeader != null && !userIdHeader.isBlank()) {
            authenticateViaPatternA(request, userIdHeader, requestId, caller);
        } else {
            // Legacy/Direct JWT support
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authenticateViaJwt(authHeader.substring(7));
            }
            
            if (request.getRequestURI().startsWith("/api/v1/internal")) {
                authenticateViaInternalHeaders(request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateViaPatternA(HttpServletRequest request, String userIdStr, String requestId, String caller) {
        try {
            Long userId = Long.parseLong(userIdStr);
            String scopes = request.getHeader("X-Scopes");
            String email = request.getHeader(USER_EMAIL_HEADER);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_INTERNAL"));

            if (scopes != null && !scopes.isBlank()) {
                authorities.addAll(Arrays.stream(scopes.split(" "))
                        .map(role -> role.trim().toUpperCase())
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            
            auth.setDetails("PatternA:" + caller + ":" + requestId + (email != null ? ":" + email : ""));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
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
        } catch (Exception e) {
        }
    }

    private void authenticateInternalService() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "INTERNAL_SERVICE", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
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
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Handle authentication for internal endpoints. We prefer to use gateway-provided
     * user headers when available (allowing the gateway to pass authenticated users),
     * otherwise fall back to marking the request as an internal service call.
     */
    private void authenticateViaInternalHeaders(HttpServletRequest request) {
        // If gateway provided user headers, authenticate as that user. Otherwise, mark as internal service.
        try {
            boolean gatewayUserAuthenticated = authenticateGatewayUser(request);
            if (!gatewayUserAuthenticated) {
                authenticateInternalService();
            }
        } catch (Exception ignored) {
            // Swallow exceptions - security failures should not break the filter chain here.
        }
    }

}
