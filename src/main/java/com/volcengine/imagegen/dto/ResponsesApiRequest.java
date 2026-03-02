package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for VolcEngine Responses API
 */
@Data
public class ResponsesApiRequest {

    /**
     * 模型ID
     */
    private String model;

    /**
     * 输入消息列表
     */
    private List<InputMessage> input;

    /**
     * 是否流式返回
     */
    private Boolean stream;

    /**
     * 采样温度
     */
    private Double temperature;

    @Data
    public static class InputMessage {
        /**
         * 角色
         */
        private String role;

        /**
         * 内容列表
         */
        private List<ContentItem> content;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentItem {
        /**
         * 类型: input_text, input_image, input_file 等
         */
        private String type;

        /**
         * 文本内容（type=input_text时使用）
         */
        private String text;

        /**
         * 图片URL（type=input_image时使用）
         */
        @JsonProperty("image_url")
        private String imageUrl;

        /**
         * 文件ID（type=input_file时使用）
         */
        @JsonProperty("file_id")
        private String fileId;

        public ContentItem(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public ContentItem(String type, String imageUrl, boolean isImage) {
            this.type = type;
            this.imageUrl = imageUrl;
        }

        public ContentItem(String type, String fileId, boolean isFile, boolean unused) {
            this.type = type;
            this.fileId = fileId;
        }
    }
}
