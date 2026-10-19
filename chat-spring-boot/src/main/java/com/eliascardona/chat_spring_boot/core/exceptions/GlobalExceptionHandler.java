package com.eliascardona.chat_spring_boot.core.exceptions;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Global exception handler for API errors */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  static class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
      super(message);
    }

    public RateLimitException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** Error response structure */
  @Getter
  public static class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final Map<String, String> details;

    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message) {
      this.timestamp = timestamp;
      this.status = status;
      this.error = error;
      this.message = message;
      this.details = new HashMap<>();
    }

    public ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details) {
      this.timestamp = timestamp;
      this.status = status;
      this.error = error;
      this.message = message;
      this.details = details;
    }

  }

  /** Handle validation errors */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });

    ErrorResponse errorResponse =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Invalid input data",
            errors);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /** Handle parameter type mismatch errors (e.g. invalid UUID format) */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex) {
    Map<String, String> details = new HashMap<>();
    String paramName = ex.getName();
    String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido";
    details.put(paramName, "Valor inválido: '" + ex.getValue() + "'. Se esperaba formato " + expectedType);

    ErrorResponse errorResponse =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_PARAMETER",
            "Parámetro de solicitud inválido",
            details);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /** Handle illegal argument errors */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex) {
    ErrorResponse errorResponse =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "INVALID_ARGUMENT",
            ex.getMessage());

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /** Handle constraint violation errors */
  @ExceptionHandler(org.hibernate.exception.ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    Map<String, String> errors = new HashMap<>();
    String violatedConstraint = ex.getConstraintName();
    errors.put("Violated constraint", violatedConstraint);

    ErrorResponse errorResponse =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Invalid input data",
            errors);

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /** Handle entity not found errors */
  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
    ErrorResponse errorResponse =
        new ErrorResponse(
            LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), "ENTITY_NOT_FOUND", ex.getMessage());

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  /** Handle all other uncaught exceptions */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
    log.error("Unhandled exception", ex);
    String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
        ? ex.getMessage()
        : "An unexpected error occurred";
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_SERVER_ERROR",
            message);
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /** Handle rate limit errors */
  @ExceptionHandler(RateLimitException.class)
  public ResponseEntity<ErrorResponse> handleRateLimitException(
      RateLimitException ex, WebRequest request) {
    log.warn("Rate limit exceeded: {}", ex.getMessage());
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.TOO_MANY_REQUESTS.value(),
            "RATE_LIMIT_EXCEEDED",
            ex.getMessage());
    return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
  }

  private ResponseEntity<Object> buildErrorResponse(
      Exception ex, HttpStatus httpStatus, WebRequest request) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", httpStatus.value());
    body.put("error", httpStatus.getReasonPhrase());
    body.put("message", ex.getMessage());
    body.put("path", request.getDescription(false).replace("uri=", ""));

    return new ResponseEntity<>(body, httpStatus);
  }
}
