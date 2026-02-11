package az.fitnest.identity.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration optimized for fast loading.
 * Uses lazy initialization and caching for better performance.
 */
@Configuration
public class OpenApiConfig {

    @Value("${springdoc.server-url:}")
    private String serverUrl;

    // Cache the OpenAPI instance since it doesn't change at runtime
    private volatile OpenAPI cachedOpenAPI;

    @Bean
    public OpenAPI customOpenAPI() {
        if (cachedOpenAPI != null) {
            return cachedOpenAPI;
        }

        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Identity Service API")
                        .version("1.0.0")
                        .description("Identity and Access Management Service - Handles authentication, registration, OTP verification, and user management")
                        .contact(new Contact()
                                .name("FitNest Team")
                                .email("support@fitnest.az")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token. Get it from /api/v1/auth/login or /api/v1/auth/register/complete")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));

        // Add server URL for Istio routing if configured
        if (serverUrl != null && !serverUrl.isEmpty()) {
            openAPI.servers(List.of(new Server().url(serverUrl).description("API Server")));
        }

        cachedOpenAPI = openAPI;
        return openAPI;
    }

    /**
     * Customizer to optimize operation processing.
     * Skips unnecessary processing for faster spec generation.
     */
    @Bean
    public OperationCustomizer operationCustomizer() {
        return (operation, handlerMethod) -> {
            // Remove null descriptions to reduce JSON size
            if (operation.getDescription() != null && operation.getDescription().isEmpty()) {
                operation.setDescription(null);
            }
            return operation;
        };
    }
}