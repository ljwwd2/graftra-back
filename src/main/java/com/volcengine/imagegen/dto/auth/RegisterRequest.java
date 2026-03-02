package com.volcengine.imagegen.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * User registration request DTO
 */
@Data
public class RegisterRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @JsonProperty("password")
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少需要8位字符")
    private String password;

    @JsonProperty("agreeToTerms")
    private boolean agreeToTerms;
}
