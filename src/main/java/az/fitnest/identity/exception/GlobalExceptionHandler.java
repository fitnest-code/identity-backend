package az.fitnest.identity.exception;

import az.fitnest.identity.model.enums.UserStatus;

import az.fitnest.identity.dto.ApiError;
import az.fitnest.identity.dto.ApiResponse;
import az.fitnest.identity.dto.FieldIssue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.*;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // ---------- OTP Rate Limit ----------
    @ExceptionHandler(OtpRateLimitedException.class)
    public ResponseEntity<ApiResponse<Void>> handleOtpRateLimitedException(
            OtpRateLimitedException exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code(exception.getErrorCode())
                .message(getMessage("error.otp_rate_limited"))
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(exception.getWaitTimeSeconds()))
                .body(ApiResponse.error(error));
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDeactivatedException(
            AccountDeactivatedException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = Map.of("otp_session_id", exception.getOtpSessionId());

        ApiError error = ApiError.builder()
                .code("ACCOUNT_DEACTIVATED")
                .message(getMessage("error.account_deactivated"))
                .status(HttpStatus.FORBIDDEN.value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(error));
    }

    // ---------- Custom BaseException ----------
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(
            BaseException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = null;

        if (exception instanceof ValidationException ve && ve.getBindingResult() != null) {
            Map<String, String> validationErrors = new LinkedHashMap<>();
            for (FieldError fieldError : ve.getBindingResult().getFieldErrors()) {
                validationErrors.putIfAbsent(fieldError.getField(), safeMessage(fieldError.getDefaultMessage()));
            }
            details = Map.of("validationErrors", validationErrors);
        }

        ApiError error = ApiError.builder()
                .code(exception.getErrorCode())
                .message(getLocalizedMessage(exception.getErrorCode(), exception.getMessage()))
                .status(exception.getHttpStatus().value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .details(details)
                .build();

        return ResponseEntity.status(exception.getHttpStatus()).body(ApiResponse.error(error));
    }

    // ---------- Bean Validation on @RequestBody ----------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldIssue> issues = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            issues.add(new FieldIssue(fieldError.getField(), safeMessage(fieldError.getDefaultMessage())));
        }

        ApiError error = ApiError.builder()
                .code("VALIDATION_ERROR")
                .message(getMessage("error.validation"))
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .details(Map.of("fieldIssues", issues))
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<FieldIssue> issues = new ArrayList<>();
        exception.getConstraintViolations().forEach(v -> {
            issues.add(new FieldIssue(
                    v.getPropertyPath() != null ? v.getPropertyPath().toString() : "param",
                    safeMessage(v.getMessage())
            ));
        });

        ApiError error = ApiError.builder()
                .code("VALIDATION_ERROR")
                .message(getMessage("error.validation"))
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .details(Map.of("fieldIssues", issues))
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    // ---------- Bad arguments ----------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("INVALID_ARGUMENT")
                .message(getMessage("error.invalid_argument"))
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    // ---------- Malformed JSON / request body ----------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        String root = rootMessage(exception);
        String userMessage = getMessage("error.invalid_json_format");

        if (root != null && root.toLowerCase(Locale.ROOT).contains("cannot deserialize")) {
            userMessage = getMessage("error.type_mismatch");
        }

        ApiError error = ApiError.builder()
                .code("INVALID_JSON")
                .message(userMessage)
                .status(HttpStatus.BAD_REQUEST.value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .details(Map.of("hint", userMessage))
                .build();

        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    // ---------- DB constraint / unique violations ----------
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        String msg = getMessage("error.data_integrity");
        String code = "DATA_INTEGRITY_VIOLATION";

        String root = Optional.ofNullable(rootMessage(exception)).orElse("");
        String lower = root.toLowerCase(Locale.ROOT);

        if (root.contains("uk_users_mobile") || lower.contains("users_mobile") || lower.contains("mobile") && lower.contains("duplicate")) {
            msg = getMessage("error.duplicate_mobile");
            code = "DUPLICATE_MOBILE";
        } else if (root.contains("uk_users_email") || lower.contains("users_email") || lower.contains("email") && lower.contains("duplicate")) {
            msg = getMessage("error.duplicate_email");
            code = "DUPLICATE_EMAIL";
        } else if (lower.contains("not-null") || lower.contains("null value") || lower.contains("violates not-null constraint")) {
            msg = getMessage("error.null_constraint");
            code = "NULL_CONSTRAINT_VIOLATION";
        }

        ApiError error = ApiError.builder()
                .code(code)
                .message(msg)
                .status(HttpStatus.CONFLICT.value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(error));
    }

    // ---------- Transaction wrapper for validation exceptions ----------
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionSystemException(
            TransactionSystemException exception,
            HttpServletRequest request
    ) {
        Throwable root = exception.getRootCause();
        if (root instanceof ConstraintViolationException cve) {
            return handleConstraintViolationException(cve, request);
        }

        ApiError error = ApiError.builder()
                .code("TRANSACTION_ERROR")
                .message(getMessage("error.transaction_error"))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception exception,
            HttpServletRequest request
    ) {
        ApiError error = ApiError.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message(getMessage("error.internal_server_error"))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(error));
    }

    private String getMessage(String code) {
        try {
            return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return code; // Fallback to code if message not found
        }
    }

    private String getLocalizedMessage(String errorCode, String defaultMessage) {
        String key = "error." + errorCode.toLowerCase();
        String message = getMessage(key);
        if (message.equals(key)) {
            // Try resolving by original errorCode
            message = getMessage(errorCode);
            if (message.equals(errorCode)) {
                return safeMessage(defaultMessage);
            }
        }
        return message;
    }

    // ---------- Helpers ----------


    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }

    private String safeMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return getMessage("error.unexpected");
        }
        // If the message looks like a key, try to resolve it
        if (msg.startsWith("error.")) {
            String resolved = getMessage(msg);
            if (!resolved.equals(msg)) {
                return resolved;
            }
        }
        return msg;
    }
}