package az.fitnest.iam.setup.adapter.client;

import az.fitnest.iam.shared.dto.ErrorWrapper;
import az.fitnest.iam.shared.exception.BadRequestException;
import az.fitnest.iam.shared.exception.ConflictException;
import az.fitnest.iam.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class UserServiceClientConfig {

    /**
     * Request interceptor that adds X-Internal-Service header for service-to-service calls.
     * This header is required by user-service to allow access to internal endpoints.
     */
    @Bean
    public RequestInterceptor internalServiceRequestInterceptor() {
        return template -> {
            template.header("X-Internal-Service", "iam-service");
            log.debug("Added X-Internal-Service header to user-service request: {}", template.url());
        };
    }

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new UserServiceErrorDecoder(objectMapper);
    }

    private static class UserServiceErrorDecoder implements ErrorDecoder {
        private final ObjectMapper objectMapper;
        private final ErrorDecoder defaultDecoder = new Default();

        public UserServiceErrorDecoder(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Exception decode(String methodKey, Response response) {
            if (response.status() >= 400) {
                try {
                    ErrorWrapper errorWrapper = parseErrorResponse(response);
                    if (errorWrapper != null && errorWrapper.getError() != null) {
                        String errorCode = errorWrapper.getError().getCode();
                        String message = errorWrapper.getError().getMessage();

                        return switch (response.status()) {
                            case 400 -> new BadRequestException(message != null ? message : "Bad request");
                            case 401 -> new az.fitnest.iam.shared.exception.UnauthorizedException(
                                    message != null ? message : "Unauthorized");
                            case 403 -> new az.fitnest.iam.shared.exception.ForbiddenException(
                                    message != null ? message : "Forbidden");
                            case 404 -> new ResourceNotFoundException(
                                    message != null ? message : "Resource not found");
                            case 409 -> {
                                if ("PROFILE_INCOMPLETE".equals(errorCode) || "SETUP_INCOMPLETE".equals(errorCode)) {
                                    yield new ConflictException(
                                            message != null ? message : "Conflict",
                                            errorCode);
                                }
                                yield new ConflictException(
                                        message != null ? message : "Conflict");
                            }
                            default -> defaultDecoder.decode(methodKey, response);
                        };
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse error response from user-service", e);
                }
            }

            return defaultDecoder.decode(methodKey, response);
        }

        private ErrorWrapper parseErrorResponse(Response response) {
            try {
                if (response.body() != null) {
                    InputStream bodyStream = response.body().asInputStream();
                    return objectMapper.readValue(bodyStream, ErrorWrapper.class);
                }
            } catch (IOException e) {
                log.debug("Could not parse error response body", e);
            }
            return null;
        }
    }
}
