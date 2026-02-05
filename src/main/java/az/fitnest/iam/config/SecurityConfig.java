package az.fitnest.iam.config;

import az.fitnest.iam.security.JwtAuthenticationFilter;
import az.fitnest.iam.security.JwtService;
import az.fitnest.iam.security.RedisTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/v3/api-docs.yml",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/swagger-ui/swagger-initializer.js",
            "/swagger-ui/swagger-ui.css",
            "/swagger-ui/swagger-ui-bundle.js",
            "/swagger-ui/swagger-ui-standalone-preset.js",
            "/swagger-ui/favicon-32x32.png",
            "/swagger-ui/favicon-16x16.png",
            "/webjars/**",
            "/webjars/swagger-ui/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security"
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
            "/health/**"
    };

    // ====================
    // Security Filter Chain
    // ====================
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OncePerRequestFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        .requestMatchers(ACTUATOR_WHITELIST).permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ====================
    // JWT Authentication Filter
    // ====================
    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter(
            JwtService jwtService,
            RedisTokenService redisTokenService,
            ObjectMapper objectMapper
    ) {
        return new JwtAuthenticationFilter(jwtService, redisTokenService, objectMapper) {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getServletPath();
                return path.startsWith("/v3/api-docs") ||
                        path.startsWith("/swagger-ui") ||
                        path.startsWith("/webjars") ||
                        path.startsWith("/actuator") ||
                        path.startsWith("/api/v1/auth/") ||
                        path.startsWith("/swagger-resources") ||
                        path.equals("/favicon.ico");
            }
        };
    }

    // ====================
    // CORS Configuration
    // ====================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*")); // Replace with your allowed domains
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

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

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("timestamp", System.currentTimeMillis());
            errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
            errorDetails.put("error", "Unauthorized");
            errorDetails.put("message", "Authentication is required to access this resource");
            errorDetails.put("path", request.getRequestURI());

            new ObjectMapper().writeValue(response.getWriter(), errorDetails);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("timestamp", System.currentTimeMillis());
            errorDetails.put("status", HttpStatus.FORBIDDEN.value());
            errorDetails.put("error", "Forbidden");
            errorDetails.put("message", "You don't have permission to access this resource");
            errorDetails.put("path", request.getRequestURI());

            new ObjectMapper().writeValue(response.getWriter(), errorDetails);
        };
    }
}