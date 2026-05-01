package de.flogehring.peelserver.error;

import de.flogehring.peel.run.exceptions.PeelException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

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
