package com.volcengine.imagegen.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response wrapper
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ErrorDetail error
) {
    /**
     * Create a success response
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, null);
    }

    /**
     * Create a success response with custom message
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, new ErrorDetail(message, "VALIDATION_ERROR"));
    }

    /**
     * Create an error response with error detail
     */
    public static <T> ApiResponse<T> error(String message, String code) {
        return new ApiResponse<>(false, message, null, new ErrorDetail(message, code));
    }

    /**
     * Error detail record
     */
    public record ErrorDetail(
            String message,
            String code
    ) {}
}
