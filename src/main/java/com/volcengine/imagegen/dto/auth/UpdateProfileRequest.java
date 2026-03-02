package com.volcengine.imagegen.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update user profile request DTO
 */
@Data
public class UpdateProfileRequest {

    @JsonProperty("name")
    @Size(max = 255, message = "昵称最多255个字符")
    private String name;

    @JsonProperty("avatar")
    @Size(max = 500, message = "头像URL最多500个字符")
    private String avatar;
}
