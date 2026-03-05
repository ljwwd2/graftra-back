package com.volcengine.imagegen.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * User login request DTO
 */
@Data
public class LoginRequest {

    @JsonProperty("phone")
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @JsonProperty("password")
    @NotBlank(message = "密码不能为空")
    private String password;

    @JsonProperty("captchaCode")
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;

    @JsonProperty("captchaId")
    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;

    @JsonProperty("remember")
    private boolean remember = false;
}
