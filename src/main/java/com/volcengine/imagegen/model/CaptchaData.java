package com.volcengine.imagegen.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Captcha data model for Redis storage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaData {

    private String code;
    private LocalDateTime expiresAt;
    private String ipAddress;
}
