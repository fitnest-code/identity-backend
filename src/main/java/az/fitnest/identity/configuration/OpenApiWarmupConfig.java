package az.fitnest.identity.configuration;

import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Configuration to warm up OpenAPI documentation at startup.
 * This pre-generates the OpenAPI spec so the first request is fast.
 */
@Configuration
public class OpenApiWarmupConfig {

    private final SpringDocConfigProperties springDocConfigProperties;
    private final OpenApiWebMvcResource openApiResource;

    @Value("${server.port:8080}")
    private int serverPort;

    @Autowired
    public OpenApiWarmupConfig(
            SpringDocConfigProperties springDocConfigProperties,
            @Autowired(required = false) OpenApiWebMvcResource openApiResource) {
        this.springDocConfigProperties = springDocConfigProperties;
        this.openApiResource = openApiResource;
    }

    /**
     * Warm up OpenAPI documentation after application startup.
     * This triggers the generation of the OpenAPI spec before any user request,
     * eliminating the slow first-request issue.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpOpenApi() {
        if (!springDocConfigProperties.getApiDocs().isEnabled()) {
            return;
        }

        try {
            // Try direct resource call first (faster, no network)
            if (openApiResource != null) {
                try {
                    openApiResource.openapiJson(null, "", Locale.getDefault());
                    return;
                } catch (Exception e) {
                    // Fall back to HTTP
                }
            }

            // Fallback: HTTP call to trigger generation
            warmupViaHttp();
        } catch (Exception e) {
            // Non-critical, don't fail startup
        }
    }

    /**
     * Warm up by making an HTTP call to the OpenAPI endpoint.
     * This is a fallback if direct resource access is not available.
     */
    private void warmupViaHttp() {
        try {
            String apiDocsPath = springDocConfigProperties.getApiDocs().getPath();
            if (apiDocsPath == null || apiDocsPath.isEmpty()) {
                apiDocsPath = "/v3/api-docs";
            }

            URL url = new URL("http://localhost:" + serverPort + apiDocsPath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Read the response to ensure it's fully generated
                connection.getInputStream().readAllBytes();
            }

            connection.disconnect();
        } catch (Exception e) {
            // Ignore warmup failures
        }
    }
}
