package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for VolcEngine Responses API
 */
@Data
public class ResponsesApiResponse {

    /**
     * 模型ID
     */
    private String model;

    /**
     * 响应ID
     */
    private String id;

    /**
     * 对象类型
     */
    private String object;

    /**
     * 创建时间
     */
    @JsonProperty("created_at")
    private Long createdAt;

    /**
     * 最大输出tokens
     */
    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;

    /**
     * 输出结果（数组）
     */
    private List<OutputItem> output;

    /**
     * 使用量
     */
    private Usage usage;

    /**
     * 错误信息（如果有）
     */
    private Error error;

    @Data
    public static class OutputItem {
        /**
         * 输出项ID
         */
        private String id;

        /**
         * 类型
         */
        private String type;

        /**
         * 摘要内容
         */
        private List<SummaryItem> summary;

        /**
         * 内容列表
         */
        private List<ContentItem> content;
    }

    @Data
    public static class SummaryItem {
        /**
         * 类型
         */
        private String type;

        /**
         * 文本内容
         */
        private String text;
    }

    @Data
    public static class ContentItem {
        /**
         * 类型
         */
        private String type;

        /**
         * 文本内容
         */
        private String text;
    }

    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    @Data
    public static class Error {
        private String message;
        private String type;
        private String code;
    }
}
