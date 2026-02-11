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

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Validate JWT directly (public or internal delegation)
            authenticateViaJwt(authHeader.substring(7));
        }

        if (request.getRequestURI().startsWith("/api/v1/internal")) {
            // Internal call - Also check for internal identity headers or mesh principal
            // This ensures ROLE_INTERNAL is granted even if a user JWT is present
            authenticateViaInternalHeaders(request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateViaInternalHeaders(HttpServletRequest request) {
        String userIdStr = request.getHeader(USER_ID_HEADER);

        // Check for valid user ID (not null, not blank, not literal "null" string)
        if (userIdStr != null && !userIdStr.isBlank() && !userIdStr.equalsIgnoreCase("null")) {
             boolean success = authenticateGatewayUser(request);
             if (!success) {
                 // Fallback to internal service authentication if user parsing fails
                 authenticateInternalService();
             }
         } else {
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
}
