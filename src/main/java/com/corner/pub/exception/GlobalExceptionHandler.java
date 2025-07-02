package com.corner.pub.exception;

import com.corner.pub.dto.response.ErrorResponse;
import com.corner.pub.exception.badrequest.BadRequestException;
import com.corner.pub.exception.conflictexception.ConflictException;
import com.corner.pub.exception.resourcenotfound.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest req) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, req);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest req) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, WebRequest req) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, req);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, WebRequest req) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, req);
    }

    // Validation errors from @Valid
    @ExceptionHandler({ MethodArgumentNotValidException.class, BindException.class })
    public ResponseEntity<ErrorResponse> handleValidationErrors(Exception ex, WebRequest req) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, req, "Validation failed");
    }

    // Type mismatch, e.g. parsing path variable
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest req) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, req, "Invalid parameter: " + ex.getName());
    }

    // Fallback for any other exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, WebRequest req) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, req, "Unexpected error");
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex,
                                                             HttpStatus status,
                                                             WebRequest req) {
        return buildErrorResponse(ex, status, req, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex,
                                                             HttpStatus status,
                                                             WebRequest req,
                                                             String message) {
        ErrorResponse err = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(err, status);
    }
}
