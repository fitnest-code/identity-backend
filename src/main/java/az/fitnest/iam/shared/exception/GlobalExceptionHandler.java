package az.fitnest.iam.shared.exception;

import az.fitnest.iam.shared.dto.ErrorResponse;
import az.fitnest.iam.shared.dto.ErrorWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BaseException.class)
	public ResponseEntity<ErrorWrapper> handleBaseException(
			BaseException exception,
			HttpServletRequest request
	) {
		ErrorResponse.ErrorResponseBuilder builder = ErrorResponse.builder()
				.message(exception.getMessage())
				.code(exception.getErrorCode())
				.path(request.getRequestURI())
				.timestamp(LocalDateTime.now());

		if (exception instanceof ValidationException ve && ve.getBindingResult() != null) {
			BindingResult result = ve.getBindingResult();
			Map<String, String> validationErrors = new HashMap<>();
			for (FieldError error : result.getFieldErrors()) {
				validationErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
			}
			builder.details(Map.of("validationErrors", validationErrors));
		}

		return ResponseEntity
				.status(exception.getHttpStatus())
				.body(ErrorWrapper.fromErrorResponse(builder.build(), exception.getHttpStatus().value()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorWrapper> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		List<ErrorWrapper.FieldIssue> details = new ArrayList<>();
		for (FieldError error : exception.getBindingResult().getFieldErrors()) {
			details.add(ErrorWrapper.FieldIssue.builder()
					.field(error.getField())
					.issue(error.getDefaultMessage())
					.build());
		}

		ErrorWrapper errorWrapper = ErrorWrapper.builder()
				.error(ErrorWrapper.ErrorDetail.builder()
						.code("VALIDATION_ERROR")
						.message("Validation failed")
						.status(HttpStatus.BAD_REQUEST.value())
						.path(request.getRequestURI())
						.timestamp(LocalDateTime.now())
						.details(details)
						.build())
				.build();

		return ResponseEntity.badRequest().body(errorWrapper);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorWrapper> handleIllegalArgument(
			IllegalArgumentException exception,
			HttpServletRequest request
	) {
		ErrorWrapper errorWrapper = ErrorWrapper.builder()
				.error(ErrorWrapper.ErrorDetail.builder()
						.code("VALIDATION_ERROR")
						.message(exception.getMessage())
						.status(HttpStatus.BAD_REQUEST.value())
						.path(request.getRequestURI())
						.timestamp(LocalDateTime.now())
						.build())
				.build();

		return ResponseEntity.badRequest().body(errorWrapper);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorWrapper> handleNotReadable(
			HttpMessageNotReadableException exception,
			HttpServletRequest request
	) {
		String detail = exception.getMostSpecificCause() != null
				? exception.getMostSpecificCause().getMessage()
				: "Malformed JSON";

		ErrorWrapper errorWrapper = ErrorWrapper.builder()
				.error(ErrorWrapper.ErrorDetail.builder()
						.code("HTTP_MESSAGE_NOT_READABLE")
						.message("Invalid request format")
						.status(HttpStatus.BAD_REQUEST.value())
						.path(request.getRequestURI())
						.timestamp(LocalDateTime.now())
						.build())
				.build();

		return ResponseEntity.badRequest().body(errorWrapper);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorWrapper> handleDataIntegrityViolation(
			DataIntegrityViolationException exception,
			HttpServletRequest request
	) {
		log.warn("Data integrity violation at {}: {}", request.getRequestURI(), exception.getMessage());
		
		String message = "Məlumat bazası xətası. Daxil edilən məlumatların unikallığını və ya tamlığını yoxlayın.";
		String code = "DATA_INTEGRITY_VIOLATION";
		
		if (exception.getMessage() != null && exception.getMessage().contains("uk_users_mobile")) {
			message = "Bu mobil nömrə artıq qeydiyyatdan keçib.";
			code = "DUPLICATE_MOBILE";
		} else if (exception.getMessage() != null && exception.getMessage().contains("uk_users_email")) {
			message = "Bu email artıq qeydiyyatdan keçib.";
			code = "DUPLICATE_EMAIL";
		} else if (exception.getMessage() != null && exception.getMessage().contains("violates not-null constraint")) {
			message = "Zəruri məlumatlar çatışmır.";
			code = "NULL_CONSTRAINT_VIOLATION";
		}

		ErrorWrapper errorWrapper = ErrorWrapper.builder()
				.error(ErrorWrapper.ErrorDetail.builder()
						.code(code)
						.message(message)
						.status(HttpStatus.CONFLICT.value())
						.path(request.getRequestURI())
						.timestamp(LocalDateTime.now())
						.build())
				.build();

		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorWrapper);
	}

	@ExceptionHandler(TransactionSystemException.class)
	public ResponseEntity<ErrorWrapper> handleTransactionSystemException(
			TransactionSystemException exception,
			HttpServletRequest request
	) {
		log.error("Transaction system exception at {}: {}", request.getRequestURI(), exception.getMessage());
		
		Throwable cause = exception.getRootCause();
		if (cause instanceof ConstraintViolationException) {
			return handleConstraintViolationException((ConstraintViolationException) cause, request);
		}

		ErrorWrapper errorWrapper = ErrorWrapper.builder()
				.error(ErrorWrapper.ErrorDetail.builder()
						.code("TRANSACTION_ERROR")
						.message("Əməliyyat zamanı xəta baş verdi.")
						.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.path(request.getRequestURI())
						.timestamp(LocalDateTime.now())
						.build())
				.build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorWrapper);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorWrapper> handleConstraintViolationException(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		List<ErrorWrapper.FieldIssue> details = new ArrayList<>();
		exception.getConstraintViolations().forEach(violation -> {
			details.add(ErrorWrapper.FieldIssue.builder()
					.field(violation.getPropertyPath().toString())
					.issue(violation.getMessage())
					.build());
		});

		ErrorWrapper errorWrapper = ErrorWrapper.builder()
				.error(ErrorWrapper.ErrorDetail.builder()
						.code("CONSTRAINT_VIOLATION")
						.message("Məlumat doğruluğu xətası")
						.status(HttpStatus.BAD_REQUEST.value())
						.path(request.getRequestURI())
						.timestamp(LocalDateTime.now())
						.details(details)
						.build())
				.build();

		return ResponseEntity.badRequest().body(errorWrapper);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorWrapper> handleGeneric(
			Exception exception,
			HttpServletRequest request
	) {
		log.error("Unhandled exception at {}: {}", request.getRequestURI(), exception.getMessage(), exception);
		ErrorWrapper errorWrapper = ErrorWrapper.builder()
				.error(ErrorWrapper.ErrorDetail.builder()
						.code("INTERNAL_SERVER_ERROR")
						.message("Internal server error")
						.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.path(request.getRequestURI())
						.timestamp(LocalDateTime.now())
						.build())
				.build();

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(errorWrapper);
	}
}