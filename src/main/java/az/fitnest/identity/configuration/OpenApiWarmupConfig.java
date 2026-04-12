package az.fitnest.identity.configuration;

import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.net.HttpURLConnection;
import java.net.URL;

@Configuration
public class OpenApiWarmupConfig {

    private final SpringDocConfigProperties springDocConfigProperties;

    @Value("${server.port:8080}")
    private int serverPort;

    @Autowired
    public OpenApiWarmupConfig(SpringDocConfigProperties springDocConfigProperties) {
        this.springDocConfigProperties = springDocConfigProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpOpenApi() {
        if (!springDocConfigProperties.getApiDocs().isEnabled()) {
            return;
        }

        try {
            // Give Tomcat a moment to fully initialize its connectors
            Thread.sleep(2000);
            warmupViaHttp();
        } catch (Exception e) {
        }
    }

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
                connection.getInputStream().readAllBytes();
            }

            connection.disconnect();
        } catch (Exception e) {
        }
    }
}
