package com.volcengine.imagegen.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * User registration request DTO
 */
@Data
public class RegisterRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("phone")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @JsonProperty("password")
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少需要8位字符")
    private String password;

    @JsonProperty("smsCode")
    @NotBlank(message = "短信验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "短信验证码必须是6位数字")
    private String smsCode;

    @JsonProperty("captchaId")
    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;

    @JsonProperty("captchaCode")
    @NotBlank(message = "图形验证码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{4}$", message = "图形验证码格式不正确")
    private String captchaCode;

    @JsonProperty("agreeToTerms")
    private boolean agreeToTerms;
}
