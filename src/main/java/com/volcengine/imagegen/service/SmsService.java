package com.volcengine.imagegen.service;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.DefaultCredentialProvider;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.volcengine.imagegen.config.SmsProperties;
import darabonba.core.client.ClientOverrideConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * SMS service for sending verification codes using Aliyun SDK
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sms", name = "enabled", havingValue = "true", matchIfMissing = false)
public class SmsService {

    private final SmsProperties smsProperties;
    private final RedisService redisService;

    /**
     * Generate a 6-digit verification code
     */
    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit number
        return String.valueOf(code);
    }

    /**
     * Send SMS verification code
     */
    public boolean sendVerificationCode(String phoneNumber) {
        if (redisService == null) {
            log.warn("Redis service not available, cannot send SMS");
            return false;
        }

        try {
            // Generate verification code
            String code = generateVerificationCode();

            // Store in Redis
            redisService.storeSmsCode(phoneNumber, code, smsProperties.getCodeExpiration());

            // Send SMS via Aliyun
            boolean sent = sendSms(phoneNumber, code);

            if (sent) {
                log.info("SMS verification code sent to: {}", phoneNumber);
                // Mark as sent for rate limiting
                redisService.markSmsSent(phoneNumber);
            } else {
                // Remove from Redis if send failed
                redisService.deleteSmsCode(phoneNumber);
            }

            return sent;

        } catch (Exception e) {
            log.error("Failed to send SMS to: {}", phoneNumber, e);
            return false;
        }
    }

    /**
     * Verify SMS code
     */
    public boolean verifyCode(String phoneNumber, String code) {
        if (redisService == null) {
            log.warn("Redis service not available");
            return false;
        }

        return redisService.verifySmsCode(phoneNumber, code);
    }

    /**
     * Send SMS via Aliyun SDK
     */
    private boolean sendSms(String phoneNumber, String code) {
        log.info("=== Sending SMS via Aliyun SDK ===");
        log.info("PhoneNumber: {}, Code: {}", phoneNumber, code);
        log.info("SignName: {}", smsProperties.getSignName());
        log.info("TemplateCode: {}", smsProperties.getTemplateCode());

        // Create client using try-with-resources
        try (AsyncClient client = createClient()) {

            // Build request - based on official example
            SendSmsVerifyCodeRequest request = SendSmsVerifyCodeRequest.builder()
                    .signName(smsProperties.getSignName())
                    .templateCode(smsProperties.getTemplateCode())
                    .phoneNumber(phoneNumber)
                    .templateParam("{\"code\":\"" + code + "\",\"min\":\"5\"}")
                    .build();

            log.info("Request built, sending to Aliyun...");

            // Send asynchronously and wait for result
            CompletableFuture<SendSmsVerifyCodeResponse> response = client.sendSmsVerifyCode(request);
            SendSmsVerifyCodeResponse resp = response.get();

            log.info("Response received: {}", resp);

            // Check if successful
            if (resp.getBody() != null) {
                String returnCode = resp.getBody().getCode();
                String message = resp.getBody().getMessage();
                Boolean success = resp.getBody().getSuccess();

                log.info("SMS response - Code: {}, Message: {}, Success: {}", returnCode, message, success);

                boolean isOk = "OK".equalsIgnoreCase(returnCode) || Boolean.TRUE.equals(success);

                if (!isOk) {
                    log.warn("SMS send failed: code={}, message={}", returnCode, message);
                }

                return isOk;
            }

            log.warn("SMS response body is null");
            return false;

        } catch (Exception e) {
            log.error("Failed to send SMS via Aliyun SDK", e);
            return false;
        }
    }

    /**
     * Create Aliyun SMS client
     */
    private AsyncClient createClient() {
        // Configure the Client - based on official example
        // The SDK will automatically read credentials from environment variables if not set
        // Or we can set them directly
        return AsyncClient.builder()
                .region(smsProperties.getRegion())
                .credentialsProvider(
                        com.aliyun.auth.credentials.provider.StaticCredentialProvider.create(
                                Credential.builder()
                                        .accessKeyId(smsProperties.getAccessKeyId())
                                        .accessKeySecret(smsProperties.getAccessKeySecret())
                                        .build()
                        )
                )
                .overrideConfiguration(
                        ClientOverrideConfiguration.create()
                                .setEndpointOverride(smsProperties.getEndpoint())
                )
                .build();
    }

    /**
     * Validate phone number format (Chinese mainland)
     */
    public boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
}
