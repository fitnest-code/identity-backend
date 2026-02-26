package az.fitnest.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FitnestSecurityFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
                                        
        String userIdStr = request.getHeader("X-User-Id");
        String authHeader = request.getHeader("Authorization");
        
        if (userIdStr != null && !userIdStr.isBlank()) {
            authenticateViaHeaders(request, userIdStr);
        } else if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authenticateViaJwt(authHeader.substring(7));
        } else if (isInternalRequest(request)) {
            authenticateAsInternalService();
        }
        
        filterChain.doFilter(request, response);
    }

    private void authenticateViaHeaders(HttpServletRequest request, String userIdStr) {
        try {
            Long userId = Long.parseLong(userIdStr);
            String scopes = request.getHeader("X-Scopes");
            String rolesHeader = request.getHeader("X-User-Roles");
            
            List<String> roles = new ArrayList<>();
            String rolesToParse = scopes != null && !scopes.isBlank() ? scopes : rolesHeader;
            
            if (rolesToParse != null && !rolesToParse.isBlank()) {
                roles.addAll(Arrays.stream(rolesToParse.split("[,\\s]+"))
                        .map(String::trim)
                        .filter(r -> !r.isEmpty())
                        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                        .toList());
            } else {
                roles.add("ROLE_USER");
            }
            
            if (isInternalRequest(request)) {
                roles.add("ROLE_INTERNAL");
            }
            
            setAuthentication(userId, roles);
        } catch (NumberFormatException e) {
            if (isInternalRequest(request)) {
                authenticateAsInternalService();
            }
        }
    }

    private void authenticateViaJwt(String token) {
        try {
            Long userId = jwtService.parseUserId(token);
            List<String> roles = jwtService.parseRoles(token).stream()
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .toList();
            setAuthentication(userId, roles);
        } catch (Exception ignored) {
        }
    }

    private void authenticateAsInternalService() {
        setAuthentication("INTERNAL_SERVICE", List.of("ROLE_INTERNAL"));
    }

    private void setAuthentication(Object principal, List<String> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
                
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, authorities);
        
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private boolean isInternalRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/v1/internal");
    }
}
