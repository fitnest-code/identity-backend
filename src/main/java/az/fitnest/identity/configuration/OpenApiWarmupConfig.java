package az.fitnest.identity.configurationuration;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
            log.debug("OpenAPI docs are disabled, skipping warmup");
            return;
        }

        try {
            log.info("Warming up OpenAPI documentation...");
            long start = System.currentTimeMillis();

            // Try direct resource call first (faster, no network)
            if (openApiResource != null) {
                try {
                    openApiResource.openapiJson(null, "", Locale.getDefault());
                    log.info("OpenAPI documentation warmed up via direct call in {}ms",
                            System.currentTimeMillis() - start);
                    return;
                } catch (Exception e) {
                    log.debug("Direct OpenAPI warmup failed, falling back to HTTP: {}", e.getMessage());
                }
            }

            // Fallback: HTTP call to trigger generation
            warmupViaHttp();

            long elapsed = System.currentTimeMillis() - start;
            log.info("OpenAPI documentation warmed up successfully in {}ms", elapsed);
        } catch (Exception e) {
            log.warn("Failed to warm up OpenAPI documentation: {}", e.getMessage());
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
                log.debug("OpenAPI warmup via HTTP completed successfully");
            } else {
                log.warn("OpenAPI warmup HTTP call returned status: {}", responseCode);
            }

            connection.disconnect();
        } catch (Exception e) {
            log.debug("HTTP warmup failed (service might still be starting): {}", e.getMessage());
        }
    }
}
