package az.fitnest.iam.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FitnestSecurityFilterTest {

    @InjectMocks
    private FitnestSecurityFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateInternalServiceViaMesh() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/internal/users/123");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("INTERNAL_SERVICE", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL")));
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateViaJwtWhenPresent() throws ServletException, IOException {
        // Arrange
        String token = "valid-jwt-token";
        when(request.getRequestURI()).thenReturn("/api/v1/auth/me"); // Non-internal endpoint
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtService.parseUserId(token)).thenReturn(123L);
        when(jwtService.parseRoles(token)).thenReturn(java.util.List.of("ROLE_USER"));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(123L, auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateViaGatewayHeadersOnInternalPath() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/internal/users/123");
        when(request.getHeader("X-User-Id")).thenReturn("456");
        when(request.getHeader("X-User-Roles")).thenReturn("ADMIN,USER");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(456L, auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        // Verify ROLE_INTERNAL is also granted for internal endpoints
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL")),
                "ROLE_INTERNAL should be granted for internal endpoint requests");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldGrantRoleInternalWhenBothJwtAndUserIdHeaderPresent() throws ServletException, IOException {
        // Arrange - This simulates the real scenario: user-service forwarding JWT + X-User-Id
        String token = "valid-jwt-token";
        when(request.getRequestURI()).thenReturn("/api/v1/internal/users/3");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-User-Id")).thenReturn("3");
        when(jwtService.parseUserId(token)).thenReturn(3L);
        when(jwtService.parseRoles(token)).thenReturn(java.util.List.of("ROLE_USER"));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set");
        assertEquals(3L, auth.getPrincipal());
        // Must have ROLE_INTERNAL for internal endpoints
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL")),
                "ROLE_INTERNAL must be granted when calling internal endpoints");
        assertTrue(auth.isAuthenticated(), "User should be authenticated");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldFallbackToInternalServiceAuthWhenUserIdHeaderIsInvalid() throws ServletException, IOException {
        // Arrange - Invalid X-User-Id header value
        when(request.getRequestURI()).thenReturn("/api/v1/internal/users/3");
        when(request.getHeader("X-User-Id")).thenReturn("invalid-not-a-number");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set even with invalid X-User-Id");
        assertEquals("INTERNAL_SERVICE", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL")),
                "ROLE_INTERNAL should be granted via fallback");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotFilterSwaggerUiEndpoints() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // Act & Assert
        assertTrue(filter.shouldNotFilter(request),
                "Swagger UI endpoints should be skipped for performance");
    }

    @Test
    void shouldNotFilterApiDocsEndpoints() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/v3/api-docs");

        // Act & Assert
        assertTrue(filter.shouldNotFilter(request),
                "OpenAPI docs endpoints should be skipped for performance");
    }

    @Test
    void shouldNotFilterActuatorEndpoints() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // Act & Assert
        assertTrue(filter.shouldNotFilter(request),
                "Actuator endpoints should be skipped for performance");
    }

    @Test
    void shouldNotFilterWebjarsEndpoints() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/webjars/swagger-ui/swagger-ui.css");

        // Act & Assert
        assertTrue(filter.shouldNotFilter(request),
                "Webjars endpoints should be skipped for performance");
    }

    @Test
    void shouldFilterApiEndpoints() {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        // Act & Assert
        assertFalse(filter.shouldNotFilter(request),
                "API endpoints should NOT be skipped");
    }
}
