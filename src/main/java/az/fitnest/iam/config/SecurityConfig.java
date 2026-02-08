package az.fitnest.iam.config;

import az.fitnest.iam.security.FitnestSecurityFilter;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
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
 * Uses a single SecurityFilterChain with proper request matchers (AntPathRequestMatcher).
 * Protects internal endpoints using InternalEndpointFilter.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info(">>> IAM-SERVICE SECURITY CONFIG LOADED - VERSION 2026-02-07-v1 <<<");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // 1. INTERNAL SERVICE-TO-SERVICE - Requires ROLE_INTERNAL
                .requestMatchers(new AntPathRequestMatcher("/api/v1/internal/**")).hasRole("INTERNAL")
                
                // 2. PUBLIC ENDPOINTS - Auth, Swagger, Actuator
                .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/login")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/refresh")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/otp/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/register")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/api/v1/auth/register/complete")).permitAll()
                
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui.html")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                
                .requestMatchers(new AntPathRequestMatcher("/actuator/**")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/health/**")).permitAll()
                
                .requestMatchers(new AntPathRequestMatcher("/error")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/**", HttpMethod.OPTIONS.name())).permitAll()
                
                // 3. ALL OTHER ENDPOINTS - Authenticated (ROLE_USER set by filter)
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )
            // Single unified security filter
            .addFilterBefore(
                new az.fitnest.iam.security.FitnestSecurityFilter(),
                UsernamePasswordAuthenticationFilter.class
            );
        
        log.info("IAM-service security configured: internal=X-Internal-Service required, external=authenticated");
        
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