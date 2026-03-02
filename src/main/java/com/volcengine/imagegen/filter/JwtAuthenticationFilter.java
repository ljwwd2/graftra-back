package com.volcengine.imagegen.filter;

import com.volcengine.imagegen.config.JwtProperties;
import com.volcengine.imagegen.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter
 * Validates JWT tokens and sets authentication context
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Skip if no Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Validate token
        if (!jwtUtil.validateToken(token)) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "无效或过期的令牌");
            return;
        }

        // Check if it's an access token
        if (!jwtUtil.isAccessToken(token)) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "令牌类型错误");
            return;
        }

        // Extract user info
        String userId = jwtUtil.getUserIdFromToken(token);
        String email = jwtUtil.getEmailFromToken(token);

        if (userId == null) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "无法解析用户信息");
            return;
        }

        // Set authentication context
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
        authentication.setDetails(email);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"code\":\"UNAUTHORIZED\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip authentication for these endpoints
        return path.startsWith("/api/auth/") &&
                (path.equals("/api/auth/register") ||
                 path.equals("/api/auth/login") ||
                 path.equals("/api/auth/wechat") ||
                 path.equals("/api/auth/health") ||
                 path.startsWith("/swagger-ui") ||
                 path.startsWith("/v3/api-docs") ||
                 path.startsWith("/uploads/"));
    }
}
