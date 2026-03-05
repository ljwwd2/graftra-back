package com.volcengine.imagegen.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.volcengine.imagegen.dto.auth.*;
import com.volcengine.imagegen.exception.AuthException;
import com.volcengine.imagegen.model.LoginMethod;
import com.volcengine.imagegen.model.RefreshToken;
import com.volcengine.imagegen.model.User;
import com.volcengine.imagegen.repository.RefreshTokenRepository;
import com.volcengine.imagegen.repository.UserRepository;
import com.volcengine.imagegen.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Authentication service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Autowired(required = false)
    private RedisService redisService;

    @Autowired(required = false)
    private SmsService smsService;

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate graphic captcha first (one-time use)
        if (redisService != null) {
            String correctCode = redisService.getCaptchaCodeForLogging(request.getCaptchaId());
            boolean isMatch = correctCode != null && correctCode.equalsIgnoreCase(request.getCaptchaCode());
            log.info("=== 图形验证码校验 === CaptchaId: {}, 用户输入: [{}], 正确验证码: [{}], 匹配: {}",
                    request.getCaptchaId(),
                    request.getCaptchaCode(),
                    correctCode,
                    isMatch);

            if (!redisService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
                throw new AuthException("图形验证码错误或已过期", "INVALID_CAPTCHA");
            }
        }

        // Validate SMS verification code
        if (smsService != null) {
            if (request.getSmsCode() == null || request.getSmsCode().isEmpty()) {
                throw new AuthException("请输入短信验证码", "SMS_CODE_REQUIRED");
            }
            if (!smsService.verifyCode(request.getPhone(), request.getSmsCode())) {
                throw new AuthException("短信验证码错误或已过期", "INVALID_SMS_CODE");
            }
            log.info("=== 短信验证码校验 === 手机号: {}, 验证通过", request.getPhone());
        }

        // Validate agree to terms
        if (!request.isAgreeToTerms()) {
            throw new AuthException("必须同意服务条款", "TERMS_NOT_AGREED");
        }

        // Check if phone already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AuthException("该手机号已被注册", "PHONE_EXISTS");
        }

        // Hash password
        String passwordHash = BCrypt.withDefaults().hashToString(12, request.getPassword().toCharArray());

        // Create user
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName() != null ? request.getName() : request.getPhone())
                .phone(request.getPhone())
                .passwordHash(passwordHash)
                .loginMethod(LoginMethod.PHONE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully - ID: {}, Phone: {}, Before flush ID: {}, After flush ID: {}",
                savedUser.getId(), savedUser.getPhone(), user.getId(), savedUser.getId());

        // Verify the user can be retrieved immediately
        userRepository.findById(savedUser.getId()).ifPresentOrElse(
                foundUser -> log.info("Verified user exists in DB - ID: {}, Phone: {}", foundUser.getId(), foundUser.getPhone()),
                () -> log.error("ERROR: User NOT found in DB immediately after save! ID: {}", savedUser.getId())
        );

        // Generate tokens
        return generateAuthResponse(savedUser);
    }

    /**
     * Login with phone and password
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Always verify captcha first (one-time use)
        if (redisService != null) {
            if (request.getCaptchaId() == null || request.getCaptchaCode() == null) {
                throw new AuthException("请输入图形验证码", "REQUIRE_CAPTCHA");
            }
            if (!redisService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
                throw new AuthException("验证码错误或已过期", "INVALID_CAPTCHA");
            }
        }

        // Find user by phone
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthException("该手机号未注册或密码错误", "INVALID_CREDENTIALS"));

        // Verify password
        boolean passwordMatches = BCrypt.verifyer()
                .verify(request.getPassword().toCharArray(), user.getPasswordHash())
                .verified;

        if (!passwordMatches) {
            throw new AuthException("该手机号未注册或密码错误", "INVALID_CREDENTIALS");
        }

        // Check if user is using phone login method
        if (user.getLoginMethod() != LoginMethod.PHONE) {
            throw new AuthException("请使用微信登录", "USE_WECHAT_LOGIN");
        }

        log.info("User logged in successfully: {}", user.getPhone());

        // Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * WeChat login
     */
    @Transactional
    public AuthResponse wechatLogin(WechatLoginRequest request) {
        // TODO: Implement WeChat OAuth flow
        // This is a placeholder implementation
        // In production, you would:
        // 1. Call WeChat API to exchange code for access_token
        // 2. Use access_token to get user info
        // 3. Create or bind user account

        // For now, throw an exception indicating this is not implemented
        throw new AuthException("微信登录暂未实现", "WECHAT_AUTH_NOT_IMPLEMENTED");

        // Example implementation (when WeChat credentials are configured):
        // String openId = wechatService.getOpenIdByCode(request.getCode());
        // User user = userRepository.findByWechatOpenid(openId)
        //     .orElseGet(() -> createWeChatUser(openId, wechatService.getUserInfo(openId)));
        // return generateAuthResponse(user);
    }

    /**
     * Refresh access token
     */
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        // Validate refresh token
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new AuthException("无效的刷新令牌", "INVALID_REFRESH_TOKEN");
        }

        if (!jwtUtil.isRefreshToken(request.getRefreshToken())) {
            throw new AuthException("令牌类型错误", "INVALID_TOKEN_TYPE");
        }

        // Find refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("刷新令牌不存在", "REFRESH_TOKEN_NOT_FOUND"));

        // Check if token is valid
        if (!refreshToken.isValid()) {
            throw new AuthException("刷新令牌已失效", "REFRESH_TOKEN_EXPIRED");
        }

        // Get user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new AuthException("该手机号未注册或密码错误", "USER_NOT_FOUND"));

        // Revoke old refresh token
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        // Generate new tokens
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getPhone());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Save new refresh token
        saveRefreshToken(user.getId(), newRefreshToken);

        log.info("Token refreshed for user: {}", user.getPhone());

        return TokenRefreshResponse.builder()
                .token(newAccessToken)
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * Logout user (revoke refresh token)
     */
    @Transactional
    public void logout(String token) {
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId != null) {
            // Revoke all refresh tokens for this user
            List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
            tokens.forEach(t -> t.setRevokedAt(LocalDateTime.now()));
            refreshTokenRepository.saveAll(tokens);
            log.info("User logged out: {}", userId);
        }
    }

    /**
     * Get current user by token
     */
    public User getCurrentUser(String token) {
        String userId = jwtUtil.getUserIdFromToken(token);
        log.info("getCurrentUser - Extracted userId from token: {}", userId);
        if (userId == null) {
            throw new AuthException("无效的令牌", "INVALID_TOKEN");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found in database for userId: {}", userId);
                    return new AuthException("该手机号未注册或密码错误", "USER_NOT_FOUND");
                });
    }

    /**
     * Change password
     */
    @Transactional
    public void changePassword(String token, ChangePasswordRequest request) {
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new AuthException("无效的令牌", "INVALID_TOKEN");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("该手机号未注册或密码错误", "USER_NOT_FOUND"));

        // Verify old password
        boolean passwordMatches = BCrypt.verifyer()
                .verify(request.getOldPassword().toCharArray(), user.getPasswordHash())
                .verified;

        if (!passwordMatches) {
            throw new AuthException("旧密码错误", "INVALID_OLD_PASSWORD");
        }

        // Check if new password is same as old password
        boolean samePassword = BCrypt.verifyer()
                .verify(request.getNewPassword().toCharArray(), user.getPasswordHash())
                .verified;

        if (samePassword) {
            throw new AuthException("新密码不能与旧密码相同", "SAME_PASSWORD");
        }

        // Update password
        String newPasswordHash = BCrypt.withDefaults().hashToString(12, request.getNewPassword().toCharArray());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        // Revoke all refresh tokens for security
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
        tokens.forEach(t -> t.setRevokedAt(LocalDateTime.now()));
        refreshTokenRepository.saveAll(tokens);

        log.info("Password changed for user: {}", user.getPhone());
    }

    /**
     * Update user profile
     */
    @Transactional
    public AuthResponse.UserDto updateProfile(String token, UpdateProfileRequest request) {
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new AuthException("无效的令牌", "INVALID_TOKEN");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("该手机号未注册或密码错误", "USER_NOT_FOUND"));

        // Update fields if provided
        boolean updated = false;
        if (request.getName() != null) {
            user.setName(request.getName());
            updated = true;
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
            updated = true;
        }

        if (updated) {
            userRepository.save(user);
            log.info("Profile updated for user: {}", user.getPhone());
        }

        return mapToUserDto(user);
    }

    /**
     * Generate authentication response
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getPhone());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Save refresh token to database
        saveRefreshToken(user.getId(), refreshToken);

        return AuthResponse.builder()
                .user(mapToUserDto(user))
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * Save refresh token to database
     */
    private void saveRefreshToken(String userId, String token) {
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusNanos(jwtUtil.getRefreshTokenExpirationMs() * 1_000_000);

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Map User entity to UserDto
     */
    private AuthResponse.UserDto mapToUserDto(User user) {
        String avatar = user.getAvatar();
        if (avatar == null && user.getWechatAvatar() != null) {
            avatar = user.getWechatAvatar();
        }

        return AuthResponse.UserDto.builder()
                .id(user.getId())
                .name(user.getName() != null ? user.getName() : user.getWechatNickname())
                .phone(user.getPhone())
                .avatar(avatar)
                .createdAt(user.getFormattedCreatedAt())
                .build();
    }

    /**
     * Send SMS verification code
     */
    public boolean sendSmsVerificationCode(String phoneNumber) {
        if (smsService == null) {
            log.warn("SMS service not available");
            return false;
        }

        // Check if phone number format is valid
        if (!smsService.isValidPhoneNumber(phoneNumber)) {
            throw new AuthException("手机号格式不正确", "INVALID_PHONE_FORMAT");
        }

        // Check if phone already exists
        if (userRepository.existsByPhone(phoneNumber)) {
            throw new AuthException("该手机号已被注册", "PHONE_EXISTS");
        }

        // Rate limiting: check if SMS was sent recently (60 seconds)
        if (redisService != null) {
            long remainingSeconds = redisService.getSmsRateLimitRemaining(phoneNumber);
            if (remainingSeconds > 0) {
                throw new AuthException(
                    String.format("验证码已发送，请%d秒后再试", remainingSeconds),
                    "SMS_CODE_RECENTLY_SENT"
                );
            }
        }

        return smsService.sendVerificationCode(phoneNumber);
    }

    /**
     * Send SMS verification code for password reset
     */
    public boolean sendSmsForResetPassword(String phoneNumber) {
        if (smsService == null) {
            log.warn("SMS service not available");
            return false;
        }

        // Check if phone number format is valid
        if (!smsService.isValidPhoneNumber(phoneNumber)) {
            throw new AuthException("手机号格式不正确", "INVALID_PHONE_FORMAT");
        }

        // Check if phone exists (user must be registered)
        if (!userRepository.existsByPhone(phoneNumber)) {
            throw new AuthException("该手机号未注册或密码错误", "USER_NOT_FOUND");
        }

        // Rate limiting
        if (redisService != null) {
            long remainingSeconds = redisService.getSmsRateLimitRemaining(phoneNumber);
            if (remainingSeconds > 0) {
                throw new AuthException(
                    String.format("验证码已发送，请%d秒后再试", remainingSeconds),
                    "SMS_CODE_RECENTLY_SENT"
                );
            }
        }

        return smsService.sendVerificationCode(phoneNumber);
    }

    /**
     * Reset password with SMS verification code
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Validate SMS verification code
        if (smsService == null) {
            throw new AuthException("短信服务不可用", "INTERNAL_ERROR");
        }

        if (!smsService.verifyCode(request.getPhone(), request.getSmsCode())) {
            throw new AuthException("短信验证码错误或已过期", "INVALID_SMS_CODE");
        }

        // Find user by phone
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AuthException("该手机号未注册或密码错误", "USER_NOT_FOUND"));

        // Hash new password
        String passwordHash = BCrypt.withDefaults().hashToString(12, request.getNewPassword().toCharArray());

        // Update password
        user.setPasswordHash(passwordHash);
        userRepository.save(user);

        log.info("Password reset successful for phone: {}", request.getPhone());
    }
}
