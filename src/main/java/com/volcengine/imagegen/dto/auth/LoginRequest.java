package com.volcengine.imagegen.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * User login request DTO
 */
@Data
public class LoginRequest {

    @JsonProperty("email")
    @NotBlank(message = "邮箱不能为空")
    private String email;

    @JsonProperty("password")
    @NotBlank(message = "密码不能为空")
    private String password;

    @JsonProperty("remember")
    private boolean remember = false;
}
