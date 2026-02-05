package az.fitnest.iam.config;

import az.fitnest.iam.config.NormalizeSlashFilter;
import az.fitnest.iam.security.JwtAuthenticationFilter;
import az.fitnest.iam.security.JwtService;
import az.fitnest.iam.security.RedisTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final ObjectMapper objectMapper;

    private static final String[] SWAGGER_WHITELIST = {
        "/iam-service/v3/api-docs/**",
        "/iam-service/v3/api-docs.yaml",
        "/iam-service/v3/api-docs.yml",
        "/iam-service/swagger-ui/**",
        "/iam-service/swagger-ui.html",
        "/iam-service/webjars/**"
};

    private static final String[] ACTUATOR_WHITELIST = {
            "/actuator",
            "/actuator/**",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/metrics"
    };

    private static final String[] AUTH_WHITELIST = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/otp/**",
            "/api/v1/auth/register/**",
            "/api/v1/auth/verify/**",
            "/api/v1/auth/password/**",
            "/api/v1/internal/**",
            "/health",
            "/health/**",
            "/favicon.ico",
            "/error"
    };

    // ====================
    // Security Filter Chain
    // ====================
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // For now, allow everything without authentication so that
                // Swagger and all APIs are publicly reachable.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // ====================
    // JWT Authentication Filter
    // ====================
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, redisTokenService, objectMapper);
    }

    // ====================
    // Normalize Slash Filter
    // ====================
    @Bean
    public NormalizeSlashFilter normalizeSlashFilter() {
        return new NormalizeSlashFilter();
    }

    // ====================
    // CORS Configuration
    // ====================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*")); // In production, replace with specific origins
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ====================
    // Authentication Manager
    // ====================
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ====================
    // Unauthorized / Access Denied Handlers
    // ====================
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("timestamp", System.currentTimeMillis());
            errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
            errorDetails.put("error", "Unauthorized");
            errorDetails.put("message", authException.getMessage());
            errorDetails.put("path", request.getRequestURI());

            objectMapper.writeValue(response.getWriter(), errorDetails);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("timestamp", System.currentTimeMillis());
            errorDetails.put("status", HttpStatus.FORBIDDEN.value());
            errorDetails.put("error", "Forbidden");
            errorDetails.put("message", accessDeniedException.getMessage());
            errorDetails.put("path", request.getRequestURI());

            objectMapper.writeValue(response.getWriter(), errorDetails);
        };
    }

    // ====================
    // HTTP Firewall
    // ====================
    // Relax the default StrictHttpFirewall to avoid rejecting URLs that contain
    // double slashes ("//") when running behind proxies / gateways like Istio.
    // Istio already normalizes and secures incoming paths, so DefaultHttpFirewall
    // is sufficient and prevents false positives for Swagger and other endpoints.
    @Bean
    public HttpFirewall httpFirewall() {
        return new DefaultHttpFirewall();
    }
}