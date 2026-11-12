package de.flogehring.peelserver.api.error;

import de.flogehring.peel.run.exceptions.PeelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(de.flogehring.peelserver.impl.files.FileConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleFileConflict(de.flogehring.peelserver.impl.files.FileConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrorResponse.of(ex.code(), ex.getMessage(), ex.details()));
    }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(ApiErrorResponse.of("project_error", ex.getReason()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest() {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("bad_request", "Invalid request fields"));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableRequest() {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("bad_request", "Invalid request body"));
    }

    @ExceptionHandler(de.flogehring.peelserver.impl.auth.AuthRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(de.flogehring.peelserver.impl.auth.AuthRequestException ex) {
        var response = ResponseEntity.status(ex.status());
        if (ex.retryAfter() > 0) response.header("Retry-After", Long.toString(ex.retryAfter()));
        return response.body(ApiErrorResponse.of(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("not_found", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of("bad_request", ex.getMessage()));
    }

    @ExceptionHandler(PeelException.class)
    public ResponseEntity<ApiErrorResponse> handlePeel(PeelException ex) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(
                "peel_error",
                ex.getMessage(),
                Map.of("category", ex.getClass().getSimpleName())
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("internal_error", "Unexpected server error"));
    }
}
