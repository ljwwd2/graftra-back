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
    @Operation(summary = "用户注册", description = "使用手机号和密码注册新用户")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for phone: {}", request.getPhone());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    /**
     * Login with phone and password
     */
    @PostMapping("/login")
    @Operation(summary = "手机号登录", description = "使用手机号和密码登录")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for phone: {}", request.getPhone());
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

        log.info("Found user - ID: {}, Phone: {}, Name: {}", user.getId(), user.getPhone(), user.getName());

        String avatar = user.getAvatar();
        if (avatar == null && user.getWechatAvatar() != null) {
            avatar = user.getWechatAvatar();
        }

        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .name(user.getName() != null ? user.getName() : user.getWechatNickname())
                .phone(user.getPhone())
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
     * Send SMS verification code
     */
    @PostMapping("/send-sms")
    @Operation(summary = "发送短信验证码", description = "发送6位数字验证码到手机号")
    public ResponseEntity<ApiResponse<Void>> sendSmsVerification(@RequestParam String phone) {
        log.info("Send SMS verification code request: {}", phone);
        boolean sent = authService.sendSmsVerificationCode(phone);
        if (sent) {
            return ResponseEntity.ok(ApiResponse.success("验证码已发送到您的手机，请查收", null));
        } else {
            return ResponseEntity.ok(ApiResponse.success("验证码发送失败，请稍后重试", null));
        }
    }

    /**
     * Send SMS for password reset
     */
    @PostMapping("/send-reset-sms")
    @Operation(summary = "发送重置密码短信验证码", description = "发送6位数字验证码到已注册的手机号")
    public ResponseEntity<ApiResponse<Void>> sendResetSms(@RequestParam String phone) {
        log.info("Send reset password SMS request: {}", phone);
        boolean sent = authService.sendSmsForResetPassword(phone);
        if (sent) {
            return ResponseEntity.ok(ApiResponse.success("验证码已发送到您的手机，请查收", null));
        } else {
            return ResponseEntity.ok(ApiResponse.success("验证码发送失败，请稍后重试", null));
        }
    }

    /**
     * Reset password with SMS code
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "通过短信验证码重置密码")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request for phone: {}", request.getPhone());
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("密码重置成功，请使用新密码登录", null));
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
