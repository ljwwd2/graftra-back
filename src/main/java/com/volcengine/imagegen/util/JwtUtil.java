package com.volcengine.imagegen.util;

import com.volcengine.imagegen.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility class for token generation and validation
 */
@Slf4j
@Component
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // Ensure the secret key is at least 256 bits (32 bytes) for HS256
        String secret = jwtProperties.getSecret();
        if (secret.length() < 32) {
            // Pad the key if it's too short
            secret = String.format("%-32s", secret).replace(' ', '0');
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate access token for a user
     */
    public String generateAccessToken(String userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("type", "access");

        return createToken(claims, userId, jwtProperties.getAccessTokenExpiration());
    }

    /**
     * Generate refresh token for a user
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");

        return createToken(claims, userId, jwtProperties.getRefreshTokenExpiration());
    }

    /**
     * Create a JWT token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .signWith(signingKey)
                .compact();
    }

    /**
     * Get user ID from token
     */
    public String getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("userId", String.class) : null;
    }

    /**
     * Get email from token
     */
    public String getEmailFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("email", String.class) : null;
    }

    /**
     * Check if token is of type "access"
     */
    public boolean isAccessToken(String token) {
        Claims claims = extractClaims(token);
        return claims != null && "access".equals(claims.get("type", String.class));
    }

    /**
     * Check if token is of type "refresh"
     */
    public boolean isRefreshToken(String token) {
        Claims claims = extractClaims(token);
        return claims != null && "refresh".equals(claims.get("type", String.class));
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract claims from token
     */
    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Failed to extract claims from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get token expiration in seconds
     */
    public Long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpiration() / 1000;
    }

    /**
     * Get refresh token expiration in milliseconds
     */
    public Long getRefreshTokenExpirationMs() {
        return jwtProperties.getRefreshTokenExpiration();
    }
}
