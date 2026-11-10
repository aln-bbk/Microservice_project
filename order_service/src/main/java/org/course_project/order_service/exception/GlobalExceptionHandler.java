package org.course_project.order_service.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(UserServiceUnavailableException.class)
  public ResponseEntity<Map<String, String>> handleUserServiceUnavailable(
      UserServiceUnavailableException ex
  ) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException ex
  ) {
    Map<String, String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            error -> error.getField(),
            error -> error.getDefaultMessage() == null
                ? "Invalid value" : error.getDefaultMessage(),
            (first, second) -> first
        ));
    return ResponseEntity.badRequest()
        .body(Map.of("error", "Validation failed", "details", errors));
  }

  @ExceptionHandler({
      HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class,
      IllegalArgumentException.class
  })
  public ResponseEntity<Map<String, String>> handleBadRequest(Exception ex) {
    return ResponseEntity.badRequest()
        .body(Map.of("error", "Invalid request"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleUnexpectedException(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("error", "Internal server error"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleResponseStatus(
      ResponseStatusException ex
  ) {
    return ResponseEntity
        .status(ex.getStatusCode())
        .body(Map.of(
            "error",
            ex.getReason() == null
                ? "Request failed"
                : ex.getReason()
        ));
  }

  @ExceptionHandler(
      DataIntegrityViolationException.class
  )
  public ResponseEntity<Map<String, String>> handleDataIntegrity(
      org.springframework.dao.DataIntegrityViolationException ex
  ) {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(Map.of(
            "error",
            "Database constraint violation"
        ));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(
      AccessDeniedException ex) {

    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(Map.of(
            "status", 403,
            "error", "Forbidden",
            "message", "Access denied"
        ));
  }
}
