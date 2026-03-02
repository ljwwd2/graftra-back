package com.volcengine.imagegen.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.imagegen.model.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for authentication endpoints
 * Prevents brute force attacks on login/register endpoints
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.auth.capacity:5}")
    private int capacity;

    @Value("${rate-limit.auth.refill-tokens:5}")
    private int refillTokens;

    @Value("${rate-limit.auth.refill-duration:60}")
    private int refillDurationSeconds;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Only apply rate limiting to auth endpoints
        if (!isAuthEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get client identifier (IP address or email from request body)
        String clientId = getClientIdentifier(request);

        // Get or create bucket for this client
        Bucket bucket = buckets.computeIfAbsent(clientId, k -> createNewBucket());

        // Try to consume a token
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client: {}", clientId);
            sendRateLimitResponse(response);
        }
    }

    /**
     * Check if the request is for an auth endpoint
     */
    private boolean isAuthEndpoint(String path) {
        return path.equals("/api/auth/register") ||
                path.equals("/api/auth/login") ||
                path.equals("/api/auth/wechat");
    }

    /**
     * Get client identifier for rate limiting
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Use IP address as the primary identifier
        String ip = getClientIp(request);

        // For login/register, also try to get email from request for better rate limiting
        String path = request.getRequestURI();
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            // Try to extract email from request body
            // Note: This won't work if the stream has been read
            // For production, consider using a request wrapper
        }

        return ip;
    }

    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    /**
     * Create a new rate limit bucket
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(capacity,
                Refill.intervally(refillTokens, Duration.ofSeconds(refillDurationSeconds)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Send rate limit exceeded response
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.error("请求过于频繁，请稍后再试", "RATE_LIMIT_EXCEEDED");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip rate limiting for non-auth endpoints
        return !isAuthEndpoint(path);
    }
}
