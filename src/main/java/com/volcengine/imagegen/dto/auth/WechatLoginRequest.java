package com.volcengine.imagegen.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * WeChat login request DTO
 */
@Data
public class WechatLoginRequest {

    @JsonProperty("code")
    @NotBlank(message = "授权码不能为空")
    private String code;

    @JsonProperty("state")
    private String state;
}
