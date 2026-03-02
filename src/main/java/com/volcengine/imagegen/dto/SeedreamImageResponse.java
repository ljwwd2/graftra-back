package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for Doubao Seedream image generation API
 */
@Data
public class SeedreamImageResponse {

    /**
     * 模型ID
     */
    private String model;

    /**
     * 创建时间戳
     */
    private Long created;

    /**
     * 生成的图片数据列表
     */
    private List<ImageData> data;

    /**
     * 使用量统计
     */
    private Usage usage;

    /**
     * 错误信息（如果请求失败）
     */
    private ErrorInfo error;

    /**
     * 检查响应是否包含错误
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * 检查是否为敏感内容错误（可重试）
     */
    public boolean isSensitiveContentError() {
        return error != null && "InputTextSensitiveContentDetected".equals(error.code);
    }

    @Data
    public static class ImageData {
        /**
         * 图片URL
         */
        private String url;

        /**
         * 图片尺寸
         */
        private String size;

        /**
         * Base64编码的图片内容 (当response_format为b64_json时返回)
         */
        private String b64Json;
    }

    @Data
    public static class Usage {
        /**
         * 生成的图片数量
         */
        @JsonProperty("generated_images")
        private Integer generatedImages;

        /**
         * 输出token数
         */
        @JsonProperty("output_tokens")
        private Integer outputTokens;

        /**
         * 总token数
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    @Data
    public static class ErrorInfo {
        /**
         * 错误码
         */
        private String code;

        /**
         * 错误信息
         */
        private String message;

        /**
         * 错误参数
         */
        private String param;

        /**
         * 错误类型
         */
        private String type;
    }
}
