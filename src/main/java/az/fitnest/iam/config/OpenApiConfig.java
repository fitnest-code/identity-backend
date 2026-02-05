package az.fitnest.iam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IAM Service API")
                        .version("1.0.0")
                        .description("Identity and Access Management Service"));
        // Security requirements removed since we're allowing all endpoints for now
    }
}