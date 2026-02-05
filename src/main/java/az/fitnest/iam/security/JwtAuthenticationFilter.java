package az.fitnest.iam.security;

import az.fitnest.iam.shared.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

// REMOVE @Component annotation - we're creating it as a bean in SecurityConfig
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7).trim();

        if (token.chars().filter(ch -> ch == '.').count() != 2) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Long userId = jwtService.parseUserId(token);

            if (!redisTokenService.isAccessTokenActive(token)) {
                writeUnauthorized(response, request.getRequestURI());
                return;
            }

            var authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, request.getRequestURI());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/webjars") ||
                path.startsWith("/actuator") ||
                path.startsWith("/api/v1/auth/") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/health") ||
                path.equals("/favicon.ico") ||
                path.equals("/error");
    }

    private void writeUnauthorized(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErrorResponse body = ErrorResponse.builder()
                .message("Unauthorized")
                .code("UNAUTHORIZED")
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}