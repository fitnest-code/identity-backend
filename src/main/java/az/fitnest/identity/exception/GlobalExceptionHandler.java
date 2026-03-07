package az.fitnest.identity.exception;

import az.fitnest.identity.dto.ApiError;
import az.fitnest.identity.dto.ApiResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDeactivatedException(AccountDeactivatedException exception, WebRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("otpSessionId", exception.getOtpSessionId());

        ApiError apiError = ApiError.builder()
                .code(exception.getErrorCode())
                .message(getLocalizedMessage(exception.getErrorCode(), exception.getMessage()))
                .status(exception.getHttpStatus().value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .details(details)
                .build();

        return ResponseEntity.status(exception.getHttpStatus()).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException exception, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .code(exception.getErrorCode())
                .message(getLocalizedMessage(exception.getErrorCode(), exception.getMessage()))
                .status(exception.getHttpStatus().value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(exception.getHttpStatus()).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, WebRequest request) {
        BindingResult result = exception.getBindingResult();
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError error : result.getFieldErrors()) {
            validationErrors.put(error.getField(), safeMessage(error.getDefaultMessage()));
        }

        ApiError apiError = ApiError.builder()
                .code("VALIDATION_ERROR")
                .message(getMessage("error.validation"))
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .details(Map.of("fieldIssues", validationErrors))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException exception, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .code("BAD_REQUEST")
                .message(getMessage("error.invalid_json_format"))
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .code("RUNTIME_EXCEPTION")
                .message(getMessage("error.unexpected"))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, WebRequest request) {
        ApiError apiError = ApiError.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message(getMessage("error.internal_server_error"))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(apiError));
    }

    private String getLocalizedMessage(String errorCode, String defaultMessage) {
        String key = "error." + errorCode.toLowerCase();
        try {
            return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
        } catch (org.springframework.context.NoSuchMessageException e1) {
            try {
                return messageSource.getMessage(errorCode, null, LocaleContextHolder.getLocale());
            } catch (org.springframework.context.NoSuchMessageException e2) {
                return safeMessage(defaultMessage);
            }
        }
    }

    private String safeMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return getMessage("error.unexpected");
        }
        if (msg.startsWith("error.")) {
            String resolved = getMessage(msg);
            if (!resolved.equals(msg)) return resolved;
        }
        return msg;
    }

    private String getMessage(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (org.springframework.context.NoSuchMessageException e) {
            return code;
        }
    }
}