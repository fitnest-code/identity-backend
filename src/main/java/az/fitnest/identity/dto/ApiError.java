package az.fitnest.identity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        Integer status,
        String path,
        OffsetDateTime timestamp,
        Object details
) {}
