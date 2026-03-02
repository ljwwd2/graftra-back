package com.volcengine.imagegen.dto;

import lombok.Data;

import java.util.List;

/**
 * Request DTO for VolcEngine Chat Completions API
 */
@Data
public class ChatCompletionRequest {

    /**
     * 模型ID
     */
    private String model;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 温度参数（0-1）
     */
    private Double temperature;

    /**
     * Top p 采样（0-1）
     */
    private Double topP;

    /**
     * 最大 token 数
     */
    private Integer maxTokens;

    /**
     * 是否流式返回
     */
    private Boolean stream;

    @Data
    public static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
