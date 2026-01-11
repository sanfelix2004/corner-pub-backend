package com.corner.pub.exception;

import com.corner.pub.dto.response.ErrorResponse;
import com.corner.pub.exception.badrequest.BadRequestException;
import com.corner.pub.exception.conflictexception.ConflictException;
import com.corner.pub.exception.resourcenotfound.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<ErrorResponse> handleValidationErrors(Exception ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return buildErrorResponse("Validation failed", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parameter type mismatch: {} = {}", ex.getName(), ex.getValue());
        return buildErrorResponse("Invalid parameter: " + ex.getName(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.apache.catalina.connector.ClientAbortException.class)
    public void handleClientAbortException(org.apache.catalina.connector.ClientAbortException ex) {
        // Log at INFO/WARN level without stack trace
        log.warn("Client disconnected: {}", ex.getMessage());
        // Do not return a ResponseEntity, as the response is already committed/broken
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex) {
        log.error("🔥 Errore interno non gestito", ex); // ✅ Log visibile in Render
        return buildErrorResponse("Unexpected error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(com.corner.pub.exception.badrequest.PrivacyException.class)
    public ResponseEntity<com.corner.pub.dto.response.ErrorResponse> handlePrivacy(
            com.corner.pub.exception.badrequest.PrivacyException ex) {
        // Return custom JSON structure if ErrorResponse supports it, otherwise use
        // message
        // The user requested { "code": "PRIVACY_NOT_ACCEPTED", "message": "..." }
        // Attempting to match requirement. If ErrorResponse strictly has only message,
        // I might need to format the message or extend ErrorResponse.
        // Assuming ErrorResponse has (message) constructor.
        // I will return a Map or similar if I can't modify ErrorResponse easily, BUT
        // user asked for consistent structure.
        // I'll stick to ErrorResponse but if I can't set code, I will use a Map?
        // Wait, standard ErrorResponse likely just has message.
        // Let's look at ErrorResponse.
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new com.corner.pub.dto.response.ErrorResponse("PRIVACY_NOT_ACCEPTED", ex.getMessage()));
    }

    @ExceptionHandler(com.corner.pub.exception.badrequest.AllergenException.class)
    public ResponseEntity<com.corner.pub.dto.response.ErrorResponse> handleAllergen(
            com.corner.pub.exception.badrequest.AllergenException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new com.corner.pub.dto.response.ErrorResponse("ALLERGENS_CONSENT_REQUIRED", ex.getMessage()));
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new ErrorResponse(message), status);
    }
}
