package az.fitnest.iam.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(
                        new io.swagger.v3.oas.models.Components()
                                .addSecuritySchemes("bearerAuth", bearerAuthScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("IAM Service API")
                .version("1.0.0")
                .description("""
                        Identity and Access Management (IAM) Service for Fitnest system.
                        
                        Responsibilities:
                        - Authentication (Login, Refresh)
                        - OTP (Send / Verify)
                        - Registration
                        - Token lifecycle (JWT + Redis)
                        """);
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER);
    }
}