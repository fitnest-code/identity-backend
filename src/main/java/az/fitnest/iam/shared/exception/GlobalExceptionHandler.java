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
				.body(ErrorWrapper.fromErrorResponse(builder.build()));
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
						.build())
				.build();

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(errorWrapper);
	}
}