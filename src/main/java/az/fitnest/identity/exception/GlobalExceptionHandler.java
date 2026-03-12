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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDeactivatedException(AccountDeactivatedException exception, WebRequest request) {
        logger.error("Account deactivated: {}", exception.getMessage(), exception);
        Map<String, Object> details = new HashMap<>();
        ApiError apiError = ApiError.builder()
                .code("error.account.deactivated")
                .message(getMessage("error.auth.account_inactive"))
                .status(HttpStatus.FORBIDDEN.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException exception, WebRequest request) {
        logger.error("Base exception: {}", exception.getMessage(), exception);
        String errorCode = exception.getErrorCode();
        String safeCode = errorCode.startsWith("error.") ? errorCode : "error.server.internal";
        String message = getMessage(safeCode);
        ApiError apiError = ApiError.builder()
                .code(safeCode)
                .message(message)
                .status(exception.getHttpStatus().value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.status(exception.getHttpStatus()).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, WebRequest request) {
        logger.error("Validation failed: {}", exception.getMessage(), exception);
        BindingResult result = exception.getBindingResult();
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError error : result.getFieldErrors()) {
            validationErrors.put(error.getField(), getMessage("error.validation.invalid_field"));
        }
        ApiError apiError = ApiError.builder()
                .code("error.validation")
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
        logger.error("Invalid JSON: {}", exception.getMessage(), exception);
        ApiError apiError = ApiError.builder()
                .code("error.request.invalid_json")
                .message(getMessage("error.request.invalid_json"))
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("Runtime exception: {}", ex.getMessage(), ex);
        ApiError apiError = ApiError.builder()
                .code("error.server.unexpected")
                .message(getMessage("error.server.unexpected"))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, WebRequest request) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        ApiError apiError = ApiError.builder()
                .code("error.server.internal")
                .message(getMessage("error.server.internal"))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(apiError));
    }

    @ExceptionHandler(OtpVerificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtpVerificationException(OtpVerificationException exception, WebRequest request) {
        logger.error("OTP verification error: {}", exception.getMessage(), exception);
        ApiError apiError = ApiError.builder()
                .code(exception.getErrorCode())
                .message(getMessage(exception.getErrorCode()))
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getDescription(false).replace("uri=", ""))
                .timestamp(OffsetDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(apiError));
    }

    private String getMessage(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (org.springframework.context.NoSuchMessageException e) {
            return getMessage("error.server.internal");
        }
    }
}
