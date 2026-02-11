package az.fitnest.identity.configurationuration;

import io.grpc.ServerBuilder;
import net.devh.boot.grpc.server.config.GrpcServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Bean
    public ServerBuilder<?> serverBuilder(GrpcServerProperties properties) {
        return ServerBuilder.forPort(properties.getPort());
    }
}
