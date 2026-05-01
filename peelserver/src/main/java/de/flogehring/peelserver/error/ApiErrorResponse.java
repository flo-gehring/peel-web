package de.flogehring.peelserver.error;

import java.util.Map;

public record ApiErrorResponse(ApiError error) {

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(new ApiError(code, message, null));
    }

    public static ApiErrorResponse of(String code, String message, Map<String, Object> details) {
        return new ApiErrorResponse(new ApiError(code, message, details));
    }

    public record ApiError(String code, String message, Map<String, Object> details) {
    }
}
