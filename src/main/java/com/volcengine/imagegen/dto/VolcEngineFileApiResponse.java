package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response DTO for VolcEngine Files API
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VolcEngineFileApiResponse {

    @JsonProperty("object")
    private String object;

    @JsonProperty("id")
    private String id;

    @JsonProperty("purpose")
    private String purpose;

    @JsonProperty("filename")
    private String filename;

    @JsonProperty("bytes")
    private Long bytes;

    @JsonProperty("mime_type")
    private String mimeType;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("expire_at")
    private Long expireAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error")
    private ErrorResponse error;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ErrorResponse {
        @JsonProperty("message")
        private String message;

        @JsonProperty("type")
        private String type;

        @JsonProperty("code")
        private String code;
    }
}
