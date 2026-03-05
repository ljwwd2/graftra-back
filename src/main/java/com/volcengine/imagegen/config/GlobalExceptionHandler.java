package com.volcengine.imagegen.config;

import com.volcengine.imagegen.exception.AuthException;
import com.volcengine.imagegen.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e) {
        log.error("Authentication error: {} - {}", e.getCode(), e.getMessage());
        HttpStatus status = getStatusByAuthCode(e.getCode());
        return ResponseEntity.status(status)
                .body(ApiResponse.error(e.getMessage(), e.getCode()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.error("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("没有访问权限", "FORBIDDEN"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("验证失败");
        log.error("Validation error: {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, "VALIDATION_ERROR"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage(), "INVALID_ARGUMENT"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("File upload size exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("File size exceeds maximum limit", "FILE_TOO_LARGE"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage(), "INTERNAL_ERROR"));
    }

    /**
     * Get HTTP status by auth error code
     */
    private HttpStatus getStatusByAuthCode(String code) {
        return switch (code) {
            case "PHONE_EXISTS", "TERMS_NOT_AGREED", "INVALID_ARGUMENT", "VALIDATION_ERROR",
                 "INVALID_OLD_PASSWORD", "SAME_PASSWORD",
                 "SMS_CODE_REQUIRED", "INVALID_SMS_CODE", "INVALID_PHONE_FORMAT", "SMS_CODE_RECENTLY_SENT",
                 "USER_NOT_FOUND" ->
                    HttpStatus.BAD_REQUEST;
            case "INVALID_CREDENTIALS", "WECHAT_AUTH_FAILED", "INVALID_TOKEN", "REFRESH_TOKEN_EXPIRED",
                 "USE_WECHAT_LOGIN" ->
                    HttpStatus.UNAUTHORIZED;
            default ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
