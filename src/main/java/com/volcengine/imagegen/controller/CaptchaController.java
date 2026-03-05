package com.volcengine.imagegen.controller;

import com.volcengine.imagegen.dto.auth.CaptchaResponse;
import com.volcengine.imagegen.model.ApiResponse;
import com.volcengine.imagegen.model.CaptchaData;
import com.volcengine.imagegen.service.RedisService;
import com.volcengine.imagegen.util.CaptchaGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Captcha controller
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "验证码", description = "图形验证码接口")
public class CaptchaController {

    private final CaptchaGenerator captchaGenerator;
    private final RedisService redisService;

    /**
     * Generate captcha
     */
    @PostMapping("/captcha")
    @Operation(summary = "生成图形验证码", description = "生成4位随机字符的图形验证码")
    public ResponseEntity<ApiResponse<CaptchaResponse>> generateCaptcha(HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        String captchaId = UUID.randomUUID().toString();

        // Generate captcha
        CaptchaGenerator.CaptchaResult result = captchaGenerator.generateCaptcha(ipAddress);

        log.info("Generated captcha - ID: {}, Code: {}, ExpiresAt: {}", captchaId, result.code(), result.expiresAt());

        // Store in Redis
        CaptchaData captchaData = CaptchaData.builder()
                .code(result.code())
                .expiresAt(result.expiresAt())
                .ipAddress(ipAddress)
                .build();
        redisService.storeCaptcha(captchaId, captchaData);
        log.info("Stored captcha in Redis - ID: {}", captchaId);

        // Build response
        CaptchaResponse response = CaptchaResponse.builder()
                .captchaId(captchaId)
                .image(result.base64Image())
                .build();

        log.info("Generated captcha for IP: {}, captchaId: {}", ipAddress, captchaId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Verify captcha (one-time use)
     * Note: If you just want to check without consuming, use login/register endpoint directly
     */
    @PostMapping("/verify-captcha")
    @Operation(summary = "验证图形验证码", description = "验证图形验证码是否正确（一次性使用）")
    public ResponseEntity<ApiResponse<Boolean>> verifyCaptcha(@RequestBody CaptchaVerifyRequest request) {
        boolean isValid = redisService.verifyCaptcha(request.captchaId(), request.captchaCode());
        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success(true));
        } else {
            return ResponseEntity.ok(ApiResponse.error("验证码错误或已过期", "INVALID_CAPTCHA"));
        }
    }

    /**
     * Captcha verify request
     */
    public record CaptchaVerifyRequest(
            String captchaId,
            String captchaCode
    ) {}

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
}
