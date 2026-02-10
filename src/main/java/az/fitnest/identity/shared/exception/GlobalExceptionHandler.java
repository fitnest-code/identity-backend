package az.fitnest.identity.shared.exception;

import az.fitnest.identity.shared.dto.ErrorResponse;
import az.fitnest.identity.shared.dto.ErrorWrapper;
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

import java.time.LocalDateTime;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- OTP Rate Limit ----------
    @ExceptionHandler(OtpRateLimitedException.class)
    public ResponseEntity<ErrorWrapper> handleOtpRateLimitedException(
            OtpRateLimitedException exception,
            HttpServletRequest request
    ) {
        ErrorWrapper body = wrap(
                exception.getErrorCode(),
                exception.getMessage(),
                HttpStatus.TOO_MANY_REQUESTS,
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(exception.getWaitTimeSeconds()))
                .body(body);
    }

    // ---------- Custom BaseException ----------
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorWrapper> handleBaseException(
            BaseException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> details = null;

        if (exception instanceof ValidationException ve && ve.getBindingResult() != null) {
            Map<String, String> validationErrors = new LinkedHashMap<>();
            for (FieldError error : ve.getBindingResult().getFieldErrors()) {
                validationErrors.putIfAbsent(error.getField(), safeMessage(error.getDefaultMessage()));
            }
            details = Map.of("validationErrors", validationErrors);
        }

        ErrorWrapper body = wrap(
                exception.getErrorCode(),
                safeMessage(exception.getMessage()),
                exception.getHttpStatus(),
                request.getRequestURI(),
                details
        );

        return ResponseEntity.status(exception.getHttpStatus()).body(body);
    }

    // ---------- Bean Validation on @RequestBody ----------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorWrapper> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ErrorWrapper.FieldIssue> issues = new ArrayList<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            issues.add(ErrorWrapper.FieldIssue.builder()
                    .field(error.getField())
                    .issue(safeMessage(error.getDefaultMessage()))
                    .build());
        }

        Map<String, Object> details = Map.of("fieldIssues", issues);

        ErrorWrapper body = wrap(
                "VALIDATION_ERROR",
                "Validation failed",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                details
        );

        return ResponseEntity.badRequest().body(body);
    }

    // ---------- @Validated on params/path/query ----------
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorWrapper> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ErrorWrapper.FieldIssue> issues = new ArrayList<>();
        exception.getConstraintViolations().forEach(v -> {
            issues.add(ErrorWrapper.FieldIssue.builder()
                    .field(v.getPropertyPath() != null ? v.getPropertyPath().toString() : "param")
                    .issue(safeMessage(v.getMessage()))
                    .build());
        });

        Map<String, Object> details = Map.of("fieldIssues", issues);

        ErrorWrapper body = wrap(
                "VALIDATION_ERROR",
                "Validation failed",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                details
        );

        return ResponseEntity.badRequest().body(body);
    }

    // ---------- Bad arguments ----------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorWrapper> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        ErrorWrapper body = wrap(
                "INVALID_ARGUMENT",
                "Invalid request data",
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                null
        );

        return ResponseEntity.badRequest().body(body);
    }

    // ---------- Malformed JSON / request body ----------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorWrapper> handleNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        // Do NOT leak raw parser message to clients by default.
        // But we can provide a safe hint for common cases.
        String root = rootMessage(exception);
        String userMessage = "Invalid request format (malformed JSON).";

        Map<String, Object> details = null;
        if (root != null && root.toLowerCase(Locale.ROOT).contains("cannot deserialize")) {
            userMessage = "Invalid request format (type mismatch).";
        } else if (root != null && root.toLowerCase(Locale.ROOT).contains("unexpected character")) {
            userMessage = "Invalid request format (malformed JSON).";
        }

        // If you *want* a safe detail field, include a generic hint, not raw stack info.
        details = Map.of("hint", userMessage);

        ErrorWrapper body = wrap(
                "INVALID_JSON",
                userMessage,
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                details
        );

        return ResponseEntity.badRequest().body(body);
    }

    // ---------- DB constraint / unique violations ----------
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorWrapper> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        String msg = "Məlumat bazası xətası. Daxil edilən məlumatların unikallığını və ya tamlığını yoxlayın.";
        String code = "DATA_INTEGRITY_VIOLATION";

        String root = Optional.ofNullable(rootMessage(exception)).orElse("");
        String lower = root.toLowerCase(Locale.ROOT);

        // Prefer matching on root-cause text; still supports your constraint names.
        if (root.contains("uk_users_mobile") || lower.contains("users_mobile") || lower.contains("mobile") && lower.contains("duplicate")) {
            msg = "Bu mobil nömrə artıq qeydiyyatdan keçib.";
            code = "DUPLICATE_MOBILE";
        } else if (root.contains("uk_users_email") || lower.contains("users_email") || lower.contains("email") && lower.contains("duplicate")) {
            msg = "Bu email artıq qeydiyyatdan keçib.";
            code = "DUPLICATE_EMAIL";
        } else if (lower.contains("not-null") || lower.contains("null value") || lower.contains("violates not-null constraint")) {
            msg = "Zəruri məlumatlar çatışmır.";
            code = "NULL_CONSTRAINT_VIOLATION";
        }

        ErrorWrapper body = wrap(
                code,
                msg,
                HttpStatus.CONFLICT,
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ---------- Transaction wrapper for validation exceptions ----------
    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ErrorWrapper> handleTransactionSystemException(
            TransactionSystemException exception,
            HttpServletRequest request
    ) {
        Throwable root = exception.getRootCause();
        if (root instanceof ConstraintViolationException cve) {
            return handleConstraintViolationException(cve, request);
        }

        ErrorWrapper body = wrap(
                "TRANSACTION_ERROR",
                "Əməliyyat zamanı xəta baş verdi.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorWrapper> handleGeneric(
            Exception exception,
            HttpServletRequest request
    ) {
        ErrorWrapper body = wrap(
                "INTERNAL_SERVER_ERROR",
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ---------- Helpers ----------
    private ErrorWrapper wrap(
            String code,
            String message,
            HttpStatus status,
            String path,
            Map<String, Object> details
    ) {
        return ErrorWrapper.builder()
                .error(ErrorWrapper.ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .status(status.value())
                        .path(path)
                        .timestamp(LocalDateTime.now())
                        .details(details) // keep it consistently Map-based
                        .build())
                .build();
    }

    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }

    private String safeMessage(String msg) {
        return (msg == null || msg.isBlank()) ? "Unexpected error" : msg;
    }
}