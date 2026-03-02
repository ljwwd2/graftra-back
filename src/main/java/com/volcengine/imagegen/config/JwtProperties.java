package com.volcengine.imagegen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT secret key
     */
    private String secret = "your-secret-key-change-this-in-production-at-least-256-bits";

    /**
     * Access token expiration time in milliseconds (default: 7 days)
     */
    private Long accessTokenExpiration = 604800000L;

    /**
     * Refresh token expiration time in milliseconds (default: 30 days)
     */
    private Long refreshTokenExpiration = 2592000000L;

    /**
     * Issuer
     */
    private String issuer = "graftra";
}
