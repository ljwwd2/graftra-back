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

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate agree to terms
        if (!request.isAgreeToTerms()) {
            throw new AuthException("必须同意服务条款", "TERMS_NOT_AGREED");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("该邮箱已被注册", "EMAIL_EXISTS");
        }

        // Hash password
        String passwordHash = BCrypt.withDefaults().hashToString(12, request.getPassword().toCharArray());

        // Create user
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName() != null ? request.getName() : extractNameFromEmail(request.getEmail()))
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .loginMethod(LoginMethod.EMAIL)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        // Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * Login with email and password
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("邮箱或密码错误", "INVALID_CREDENTIALS"));

        // Verify password
        boolean passwordMatches = BCrypt.verifyer()
                .verify(request.getPassword().toCharArray(), user.getPasswordHash())
                .verified;

        if (!passwordMatches) {
            throw new AuthException("邮箱或密码错误", "INVALID_CREDENTIALS");
        }

        // Check if user is using email login method
        if (user.getLoginMethod() != LoginMethod.EMAIL) {
            throw new AuthException("请使用微信登录", "USE_WECHAT_LOGIN");
        }

        log.info("User logged in successfully: {}", user.getEmail());

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
                .orElseThrow(() -> new AuthException("用户不存在", "USER_NOT_FOUND"));

        // Revoke old refresh token
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        // Generate new tokens
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        // Save new refresh token
        saveRefreshToken(user.getId(), newRefreshToken);

        log.info("Token refreshed for user: {}", user.getEmail());

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
        if (userId == null) {
            throw new AuthException("无效的令牌", "INVALID_TOKEN");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("用户不存在", "USER_NOT_FOUND"));
    }

    /**
     * Generate authentication response
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
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
                .email(user.getEmail())
                .avatar(avatar)
                .createdAt(user.getFormattedCreatedAt())
                .build();
    }

    /**
     * Extract name from email
     */
    private String extractNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }
        return email;
    }
}
