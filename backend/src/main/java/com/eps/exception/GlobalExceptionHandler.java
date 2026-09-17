package com.eps.exception;

import com.eps.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * Handles all exceptions across the application and returns consistent error responses
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handle User Not Found Exception
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponseDto> handleUserNotFoundException(
            UserNotFoundException ex, WebRequest request) {
        log.error("User Not Found: {}", ex.getMessage());
        ex.printStackTrace();
        
        ApiResponseDto response = ApiResponseDto.builder()
                .success(false)
                .message(ex.getMessage())
                .error("USER_NOT_FOUND")
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle Email Already Exists Exception
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponseDto> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex, WebRequest request) {
        log.error("Email Already Exists: {}", ex.getMessage());
        ex.printStackTrace();
        
        ApiResponseDto response = ApiResponseDto.builder()
                .success(false)
                .message(ex.getMessage())
                .error("EMAIL_ALREADY_EXISTS")
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    /**
     * Handle Invalid Password Exception
     */
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponseDto> handleInvalidPasswordException(
            InvalidPasswordException ex, WebRequest request) {
        log.error("Invalid Password: {}", ex.getMessage());
        ex.printStackTrace();
        
        ApiResponseDto response = ApiResponseDto.builder()
                .success(false)
                .message(ex.getMessage())
                .error("INVALID_PASSWORD")
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Handle Validation Errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation Error: {}", ex.getMessage());
        ex.printStackTrace();
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponseDto response = ApiResponseDto.builder()
                .success(false)
                .message("Validation failed")
                .error("VALIDATION_ERROR")
                .data(errors)
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle Generic Exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("An error occurred: ", ex);
        ex.printStackTrace();
        
        ApiResponseDto response = ApiResponseDto.builder()
                .success(false)
                .message(ex.getMessage() != null ? ex.getMessage() : ex.toString())
                .error(ex.getClass().getSimpleName())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}
