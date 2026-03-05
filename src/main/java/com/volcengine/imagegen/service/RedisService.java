package com.volcengine.imagegen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.imagegen.model.CaptchaData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis service for captcha and session management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(RedisTemplate.class)
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Key prefixes
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String LOGIN_FAIL_PREFIX = "login_fail:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token_blacklist:";
    private static final String SMS_CODE_PREFIX = "sms_code:";
    private static final String SMS_LAST_SENT_PREFIX = "sms_last_sent:";

    // TTL values
    private static final long CAPTCHA_TTL_SECONDS = 300;  // 5 minutes
    private static final long LOGIN_FAIL_TTL_SECONDS = 900; // 15 minutes
    private static final long TOKEN_BLACKLIST_TTL_SECONDS = 3600; // 1 hour
    private static final long SMS_RATE_LIMIT_SECONDS = 60; // 1 minute

    /**
     * Store captcha in Redis
     */
    public void storeCaptcha(String captchaId, CaptchaData captchaData) {
        String key = CAPTCHA_PREFIX + captchaId;
        redisTemplate.opsForValue().set(key, captchaData, CAPTCHA_TTL_SECONDS, TimeUnit.SECONDS);
        log.info("Stored captcha in Redis - Key: {}, Code: {}, ExpiresAt: {}", key, captchaData.getCode(), captchaData.getExpiresAt());
    }

    /**
     * Get captcha from Redis
     */
    public CaptchaData getCaptcha(String captchaId) {
        String key = CAPTCHA_PREFIX + captchaId;
        Object data = redisTemplate.opsForValue().get(key);
        log.info("Get captcha from Redis - Key: {}, Found: {}, DataType: {}", key, data != null, data != null ? data.getClass() : "null");

        if (data instanceof CaptchaData) {
            CaptchaData captchaData = (CaptchaData) data;
            log.info("Retrieved captcha as CaptchaData - Code: {}, ExpiresAt: {}", captchaData.getCode(), captchaData.getExpiresAt());
            return captchaData;
        }

        // Handle LinkedHashMap case (for backward compatibility or serialization issues)
        if (data instanceof Map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) data;
                CaptchaData captchaData = objectMapper.convertValue(map, CaptchaData.class);
                log.info("Converted LinkedHashMap to CaptchaData - Code: {}, ExpiresAt: {}", captchaData.getCode(), captchaData.getExpiresAt());
                return captchaData;
            } catch (Exception e) {
                log.error("Failed to convert Map to CaptchaData: {}", e.getMessage());
            }
        }

        if (data != null) {
            log.warn("Captcha data is not CaptchaData type: {}", data.getClass());
        }
        return null;
    }

    /**
     * Delete captcha from Redis
     */
    public void deleteCaptcha(String captchaId) {
        String key = CAPTCHA_PREFIX + captchaId;
        redisTemplate.delete(key);
        log.info("Deleted captcha: {}", captchaId);
    }

    /**
     * Verify captcha code
     */
    public boolean verifyCaptcha(String captchaId, String userInputCode) {
        log.info("Verifying captcha - ID: {}, Input: {}", captchaId, userInputCode);
        CaptchaData captchaData = getCaptcha(captchaId);
        if (captchaData == null) {
            log.warn("Captcha not found or expired: {}", captchaId);
            return false;
        }

        // Check expiration
        if (LocalDateTime.now().isAfter(captchaData.getExpiresAt())) {
            deleteCaptcha(captchaId);
            log.warn("Captcha expired: {}", captchaId);
            return false;
        }

        // Compare code (case-insensitive)
        boolean isValid = captchaData.getCode().equalsIgnoreCase(userInputCode);
        log.info("Captcha comparison - Stored: {}, Input: {}, Valid: {}", captchaData.getCode(), userInputCode, isValid);

        // Delete after use (one-time use)
        if (isValid) {
            deleteCaptcha(captchaId);
        }

        return isValid;
    }

    /**
     * Check captcha without consuming it (for validation before final commit)
     */
    public boolean checkCaptcha(String captchaId, String userInputCode) {
        log.info("Checking captcha (non-consuming) - ID: {}, Input: {}", captchaId, userInputCode);
        CaptchaData captchaData = getCaptcha(captchaId);
        if (captchaData == null) {
            log.warn("Captcha not found or expired: {}", captchaId);
            return false;
        }

        // Check expiration
        if (LocalDateTime.now().isAfter(captchaData.getExpiresAt())) {
            log.warn("Captcha expired: {}", captchaId);
            return false;
        }

        // Compare code (case-insensitive)
        boolean isValid = captchaData.getCode().equalsIgnoreCase(userInputCode);
        log.info("Captcha check - Stored: {}, Input: {}, Valid: {}", captchaData.getCode(), userInputCode, isValid);

        return isValid;
    }

    /**
     * Get captcha code for logging (non-consuming)
     */
    public String getCaptchaCodeForLogging(String captchaId) {
        String key = CAPTCHA_PREFIX + captchaId;
        Object data = redisTemplate.opsForValue().get(key);

        if (data instanceof CaptchaData) {
            return ((CaptchaData) data).getCode();
        }

        // Handle LinkedHashMap case
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            Object code = map.get("code");
            return code != null ? code.toString() : null;
        }

        return null;
    }

    /**
     * Increment login failure count
     */
    public int incrementLoginFailures(String email) {
        String key = LOGIN_FAIL_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null) {
            // Set expiration on first failure
            if (count == 1) {
                redisTemplate.expire(key, LOGIN_FAIL_TTL_SECONDS, TimeUnit.SECONDS);
            }
            log.debug("Login failure count for {}: {}", email, count);
            return count.intValue();
        }
        return 0;
    }

    /**
     * Get login failure count
     */
    public int getLoginFailureCount(String email) {
        String key = LOGIN_FAIL_PREFIX + email;
        Object count = redisTemplate.opsForValue().get(key);
        if (count instanceof Integer) {
            return (Integer) count;
        } else if (count instanceof Long) {
            return ((Long) count).intValue();
        }
        return 0;
    }

    /**
     * Clear login failure count
     */
    public void clearLoginFailures(String email) {
        String key = LOGIN_FAIL_PREFIX + email;
        redisTemplate.delete(key);
        log.debug("Cleared login failures for: {}", email);
    }

    /**
     * Check if account is locked (too many failures)
     */
    public boolean isAccountLocked(String email) {
        return getLoginFailureCount(email) >= 10;
    }

    /**
     * Get remaining lock time in seconds
     */
    public long getRemainingLockTime(String email) {
        String key = LOGIN_FAIL_PREFIX + email;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    /**
     * Add token to blacklist
     */
    public void addTokenToBlacklist(String tokenId, long expirationSeconds) {
        String key = TOKEN_BLACKLIST_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, "1", expirationSeconds, TimeUnit.SECONDS);
        log.debug("Added token to blacklist: {}", tokenId);
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String tokenId) {
        String key = TOKEN_BLACKLIST_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Store SMS verification code in Redis
     */
    public void storeSmsCode(String phoneNumber, String code, long ttlSeconds) {
        String key = SMS_CODE_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(key, code, ttlSeconds, TimeUnit.SECONDS);
        log.info("Stored SMS code for phone: {}, expires in {} seconds", phoneNumber, ttlSeconds);
    }

    /**
     * Get SMS verification code from Redis
     */
    private String getSmsCode(String phoneNumber) {
        String key = SMS_CODE_PREFIX + phoneNumber;
        Object code = redisTemplate.opsForValue().get(key);
        return code != null ? code.toString() : null;
    }

    /**
     * Delete SMS verification code from Redis
     */
    public void deleteSmsCode(String phoneNumber) {
        String key = SMS_CODE_PREFIX + phoneNumber;
        redisTemplate.delete(key);
        log.debug("Deleted SMS code for phone: {}", phoneNumber);
    }

    /**
     * Verify SMS verification code
     */
    public boolean verifySmsCode(String phoneNumber, String inputCode) {
        String storedCode = getSmsCode(phoneNumber);
        if (storedCode == null) {
            log.warn("SMS code not found or expired for phone: {}", phoneNumber);
            return false;
        }

        boolean isValid = storedCode.equals(inputCode);

        if (isValid) {
            deleteSmsCode(phoneNumber); // One-time use
            log.info("SMS code verified successfully for phone: {}", phoneNumber);
        } else {
            log.warn("SMS code mismatch for phone: {}, expected: {}, got: {}", phoneNumber, storedCode, inputCode);
        }

        return isValid;
    }

    /**
     * Check if SMS was sent recently (for rate limiting)
     * Returns remaining seconds to wait, 0 if can send
     */
    public long getSmsRateLimitRemaining(String phoneNumber) {
        String key = SMS_LAST_SENT_PREFIX + phoneNumber;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            return 0;
        }
        return ttl;
    }

    /**
     * Mark SMS as sent (for rate limiting)
     */
    public void markSmsSent(String phoneNumber) {
        String key = SMS_LAST_SENT_PREFIX + phoneNumber;
        redisTemplate.opsForValue().set(key, "1", SMS_RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        log.info("Marked SMS as sent for phone: {}, rate limit: {} seconds", phoneNumber, SMS_RATE_LIMIT_SECONDS);
    }
}
