package com.infy.inventrack.user.exception;

import com.infy.inventrack.common.dto.ErrorResponse;
import com.infy.inventrack.common.exception.BusinessValidationException;
import com.infy.inventrack.common.exception.DownstreamServiceException;
import com.infy.inventrack.common.exception.DuplicateResourceException;
import com.infy.inventrack.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    ResponseEntity<ErrorResponse> duplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler({BusinessValidationException.class, ConstraintViolationException.class})
    ResponseEntity<ErrorResponse> validation(RuntimeException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request);
    }

    @ExceptionHandler(DownstreamServiceException.class)
    ResponseEntity<ErrorResponse> downstream(DownstreamServiceException ex, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "DOWNSTREAM_UNAVAILABLE", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + " " + field.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(), status.value(), code, message, request.getRequestURI()));
    }
}
