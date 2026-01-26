package az.fitnest.iam.shared.exception;

import az.fitnest.iam.shared.dto.ErrorResponse;
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
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BaseException.class)
	public ResponseEntity<ErrorResponse> handleBaseException(
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
				.body(builder.build());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		Map<String, String> validationErrors = new HashMap<>();
		for (FieldError error : exception.getBindingResult().getFieldErrors()) {
			validationErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
		}

		ErrorResponse response = ErrorResponse.builder()
				.message("Validation failed")
				.code("VALIDATION_ERROR")
				.path(request.getRequestURI())
				.timestamp(LocalDateTime.now())
				.details(Map.of("validationErrors", validationErrors))
				.build();

		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(
			IllegalArgumentException exception,
			HttpServletRequest request
	) {
		ErrorResponse response = ErrorResponse.builder()
				.message(exception.getMessage())
				.code("VALIDATION_ERROR")
				.path(request.getRequestURI())
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleNotReadable(
			HttpMessageNotReadableException exception,
			HttpServletRequest request
	) {
		String detail = exception.getMostSpecificCause() != null
				? exception.getMostSpecificCause().getMessage()
				: "Malformed JSON";

		ErrorResponse response = ErrorResponse.builder()
				.message("Invalid request format")
				.code("HTTP_MESSAGE_NOT_READABLE")
				.path(request.getRequestURI())
				.timestamp(LocalDateTime.now())
				.details(Map.of("cause", detail))
				.build();

		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(
			Exception exception,
			HttpServletRequest request
	) {
		ErrorResponse response = ErrorResponse.builder()
				.message("Internal server error")
				.code("INTERNAL_SERVER_ERROR")
				.path(request.getRequestURI())
				.timestamp(LocalDateTime.now())
				.build();

		return ResponseEntity
				.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(response);
	}
}