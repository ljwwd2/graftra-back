package com.volcengine.imagegen.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Change password request DTO
 */
@Data
public class ChangePasswordRequest {

    @JsonProperty("oldPassword")
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @JsonProperty("newPassword")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码至少需要8位字符")
    private String newPassword;
}
