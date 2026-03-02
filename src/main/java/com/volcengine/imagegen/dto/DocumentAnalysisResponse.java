package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for document analysis
 */
@Data
public class DocumentAnalysisResponse {

    /**
     * 模型ID
     */
    private String model;

    /**
     * 创建时间戳
     */
    private Long created;

    /**
     * 分析结果列表
     */
    private List<Choice> choices;

    /**
     * 使用量统计
     */
    private Usage usage;

    /**
     * 错误信息（如果有）
     */
    private Error error;

    @Data
    public static class Choice {
        /**
         * 索引
         */
        private Integer index;

        /**
         * 消息内容
         */
        private Message message;

        /**
         * 完成原因
         */
        private String finishReason;
    }

    @Data
    public static class Message {
        /**
         * 角色
         */
        private String role;

        /**
         * 内容
         */
        private String content;
    }

    @Data
    public static class Usage {
        /**
         * 提示词 token 数
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * 完成 token 数
         */
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        /**
         * 总 token 数
         */
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
