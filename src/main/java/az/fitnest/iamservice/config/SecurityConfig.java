package az.fitnest.iamservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
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

            try {
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
                errorDetails.put("error", "Unauthorized");
                errorDetails.put("message", "Authentication is required to access this resource");
                errorDetails.put("path", request.getRequestURI());

                ObjectMapper mapper = new ObjectMapper();
                response.getWriter().write(mapper.writeValueAsString(errorDetails));
            } catch (Exception e) {
                response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication is required\"}");
            }
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            try {
                Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("status", HttpStatus.FORBIDDEN.value());
                errorDetails.put("error", "Forbidden");
                errorDetails.put("message", "You don't have permission to access this resource");
                errorDetails.put("path", request.getRequestURI());

                ObjectMapper mapper = new ObjectMapper();
                response.getWriter().write(mapper.writeValueAsString(errorDetails));
            } catch (Exception e) {
                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
            }
        };
    }
}
