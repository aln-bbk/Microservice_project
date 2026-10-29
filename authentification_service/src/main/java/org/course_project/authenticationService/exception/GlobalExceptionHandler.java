package org.course_project.authenticationService.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(
      BadCredentialsException exception
  ) {
    return error(
        HttpStatus.UNAUTHORIZED,
        "Invalid login, password or token",
        Map.of()
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException exception
  ) {
    Map<String, String> errors = new LinkedHashMap<>();

    exception.getBindingResult().getFieldErrors().forEach(fieldError ->
        errors.putIfAbsent(
            fieldError.getField(),
            fieldError.getDefaultMessage() == null
                ? "Invalid value"
                : fieldError.getDefaultMessage()
        )
    );

    return error(
        HttpStatus.BAD_REQUEST,
        "Request validation failed",
        errors
    );
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(
      ConstraintViolationException exception
  ) {
    return error(
        HttpStatus.BAD_REQUEST,
        "Request validation failed",
        Map.of()
    );
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableMessage(
      HttpMessageNotReadableException exception
  ) {
    return error(
        HttpStatus.BAD_REQUEST,
        "Request body is missing or contains invalid JSON",
        Map.of()
    );
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> handleResponseStatus(
      ResponseStatusException exception
  ) {
    HttpStatus status = HttpStatus.valueOf(
        exception.getStatusCode().value()
    );

    return error(
        status,
        exception.getReason() == null
            ? status.getReasonPhrase()
            : exception.getReason(),
        Map.of()
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrity(
      DataIntegrityViolationException exception
  ) {
    return error(
        HttpStatus.CONFLICT,
        "A credential with this login or user ID already exists",
        Map.of()
    );
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(
      AccessDeniedException exception
  ) {
    return error(
        HttpStatus.FORBIDDEN,
        "You do not have permission to perform this action",
        Map.of()
    );
  }

  private ResponseEntity<ApiError> error(
      HttpStatus status,
      String message,
      Map<String, String> validationErrors
  ) {
    ApiError body = new ApiError(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        validationErrors
    );

    return ResponseEntity.status(status).body(body);
  }
}
