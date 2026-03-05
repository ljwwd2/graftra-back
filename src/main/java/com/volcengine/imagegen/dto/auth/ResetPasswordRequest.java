package com.volcengine.imagegen.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Reset password request DTO (for forgot password flow)
 */
@Data
public class ResetPasswordRequest {

    @JsonProperty("phone")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @JsonProperty("smsCode")
    @NotBlank(message = "短信验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "短信验证码必须是6位数字")
    private String smsCode;

    @JsonProperty("newPassword")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码至少需要8位字符")
    private String newPassword;
}
