package com.volcengine.imagegen.controller;

import com.volcengine.imagegen.dto.auth.*;
import com.volcengine.imagegen.model.ApiResponse;
import com.volcengine.imagegen.model.User;
import com.volcengine.imagegen.service.AuthService;
import com.volcengine.imagegen.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "用户认证接口")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "使用邮箱和密码注册新用户")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    /**
     * Login with email and password
     */
    @PostMapping("/login")
    @Operation(summary = "邮箱登录", description = "使用邮箱和密码登录")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * WeChat login
     */
    @PostMapping("/wechat")
    @Operation(summary = "微信登录", description = "使用微信授权码登录")
    public ResponseEntity<ApiResponse<AuthResponse>> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        log.info("WeChat login request");
        AuthResponse response = authService.wechatLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Logout user
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "退出当前用户登录状态")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authorization) {
        String token = extractToken(authorization);
        log.info("Logout request");
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("退出成功", null));
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get current user information
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public ResponseEntity<ApiResponse<AuthResponse.UserDto>> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        String token = extractToken(authorization);
        log.info("GET /api/auth/me - Token length: {}, First 20 chars: {}", token.length(), token.substring(0, Math.min(20, token.length())));

        User user = authService.getCurrentUser(token);

        log.info("Found user - ID: {}, Email: {}, Name: {}", user.getId(), user.getEmail(), user.getName());

        String avatar = user.getAvatar();
        if (avatar == null && user.getWechatAvatar() != null) {
            avatar = user.getWechatAvatar();
        }

        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .name(user.getName() != null ? user.getName() : user.getWechatNickname())
                .email(user.getEmail())
                .avatar(avatar)
                .createdAt(user.getFormattedCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    /**
     * Update user profile (partial update)
     */
    @PatchMapping("/me")
    @Operation(summary = "更新用户信息", description = "更新当前用户的昵称或头像")
    public ResponseEntity<ApiResponse<AuthResponse.UserDto>> updateProfile(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UpdateProfileRequest request) {
        String token = extractToken(authorization);
        log.info("PATCH /api/auth/me - Update profile request");
        AuthResponse.UserDto userDto = authService.updateProfile(token, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", userDto));
    }

    /**
     * Change password
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前用户密码（需要验证旧密码）")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ChangePasswordRequest request) {
        String token = extractToken(authorization);
        log.info("PUT /api/auth/password - Change password request");
        authService.changePassword(token, request);
        return ResponseEntity.ok(ApiResponse.success("密码修改成功，请重新登录", null));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查认证服务状态")
    public ResponseEntity<ApiResponse<Void>> health() {
        return ResponseEntity.ok(ApiResponse.success("OK", null));
    }

    /**
     * Extract Bearer token from Authorization header
     */
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
