package az.fitnest.iam.config;

import az.fitnest.iam.security.gateway.GatewayHeaderAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Security configuration for iam-service.
 * 
 * Uses a single SecurityFilterChain with proper request matchers
 * to handle both public, internal (service-to-service), and authenticated requests.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    /**
     * Public endpoints that don't require authentication.
     * Order matters - more specific patterns should come first.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            // Internal service-to-service endpoints - NO authentication required
            "/api/v1/internal/**",
            
            // Auth endpoints - public by design
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/otp/**",
            "/api/v1/auth/register",
            "/api/v1/auth/register/complete",
            
            // Swagger/OpenAPI
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            
            // Actuator and health
            "/actuator/**",
            "/health/**",
            
            // Error handling
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring iam-service security filter chain");
        
        http
            // Disable CSRF for stateless REST API
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow all public endpoints without authentication
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                
                // Allow OPTIONS requests for CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Exception handling with custom responses
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            
            // Add custom authentication filter for gateway headers
            .addFilterBefore(
                new GatewayHeaderAuthenticationFilter(), 
                UsernamePasswordAuthenticationFilter.class
            );
        
        log.info("IAM-service security configured. Public endpoints: {}", 
                 Arrays.toString(PUBLIC_ENDPOINTS));
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(
            "Authorization", "X-User-Id", "X-User-Email", "X-User-Roles", "X-Request-ID"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> error = new HashMap<>();
            error.put("status", 401);
            error.put("error", "Unauthorized");
            error.put("message", "Authentication required");
            error.put("path", request.getRequestURI());
            objectMapper.writeValue(response.getWriter(), error);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> error = new HashMap<>();
            error.put("status", 403);
            error.put("error", "Forbidden");
            error.put("message", accessDeniedException.getMessage());
            error.put("path", request.getRequestURI());
            objectMapper.writeValue(response.getWriter(), error);
        };
    }

    @Bean
    public HttpFirewall httpFirewall() {
        return new DefaultHttpFirewall();
    }
}