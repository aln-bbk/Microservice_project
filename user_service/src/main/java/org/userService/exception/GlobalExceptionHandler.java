package org.userService.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler({
      EmailAlreadyExistsException.class,
      CardLimitExceededException.class,
      CardNumberAlreadyExistsException.class,
      InvalidResourceStateException.class
  })
  public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex) {
    return error(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException ex
  ) {
    String message = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(e -> e.getField() + ": " + e.getDefaultMessage())
        .collect(Collectors.joining("; "));

    return error(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler({
      MethodArgumentTypeMismatchException.class,
      HttpMessageNotReadableException.class
  })
  public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
    return error(
        HttpStatus.BAD_REQUEST,
        "Invalid request parameter or request body"
    );
  }

  @ExceptionHandler({AuthServiceIntegrationException.class, RestClientException.class})
  public ResponseEntity<Map<String, Object>> handleAuthServiceUnavailable(RuntimeException ex) {
    return error(
        HttpStatus.BAD_GATEWAY,
        "Could not save credentials in Auth Service; please retry registration"
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrity(
      DataIntegrityViolationException ex
  ) {
    return error(
        HttpStatus.CONFLICT,
        "The request violates a database constraint"
    );
  }

  private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Map.of(
        "timestamp", OffsetDateTime.now().toString(),
        "status", status.value(),
        "error", status.getReasonPhrase(),
        "message", message
    ));
  }
}