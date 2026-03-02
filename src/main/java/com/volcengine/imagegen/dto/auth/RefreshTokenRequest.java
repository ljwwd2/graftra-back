package com.volcengine.imagegen.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh token request DTO
 */
@Data
public class RefreshTokenRequest {

    @JsonProperty("refreshToken")
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
