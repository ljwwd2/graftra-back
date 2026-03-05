package com.volcengine.imagegen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SMS configuration properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /**
     * Enable SMS sending
     */
    private boolean enabled = true;

    /**
     * Aliyun Access Key ID
     */
    private String accessKeyId;

    /**
     * Aliyun Access Key Secret
     */
    private String accessKeySecret;

    /**
     * Aliyun SMS endpoint
     */
    private String endpoint = "dypnsapi.aliyuncs.com";

    /**
     * Region
     */
    private String region = "cn-shanghai";

    /**
     * Sign name
     */
    private String signName;

    /**
     * Verification code template code
     */
    private String templateCode;

    /**
     * Verification code expiration in seconds (default 5 minutes)
     */
    private long codeExpiration = 300;
}
