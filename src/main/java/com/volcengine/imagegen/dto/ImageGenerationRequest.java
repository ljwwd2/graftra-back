package com.volcengine.imagegen.dto;

import lombok.Data;

/**
 * Request DTO for image generation API
 */
@Data
public class ImageGenerationRequest {

    /**
     * Model ID
     */
    private String model;

    /**
     * List of messages
     */
    private java.util.List<Message> messages;

    /**
     * Temperature (0-1)
     */
    private Double temperature;

    /**
     * Top p (0-1)
     */
    private Double topP;

    /**
     * Maximum tokens
     */
    private Integer maxTokens;

    /**
     * Stream response
     */
    private Boolean stream;

    @Data
    public static class Message {
        private String role;
        private Content content;

        public Message(String role, Content content) {
            this.role = role;
            this.content = content;
        }

        public Message(String role, String text) {
            this.role = role;
            this.content = new Content(text);
        }
    }

    @Data
    public static class Content {
        private String text;
        private java.util.List<ContentPart> parts;

        public Content(String text) {
            this.text = text;
        }

        public Content(java.util.List<ContentPart> parts) {
            this.parts = parts;
        }
    }

    @Data
    public static class ContentPart {
        private String type;
        private String text;
        private ImageUrl image_url;

        public ContentPart(String text) {
            this.type = "text";
            this.text = text;
        }

        public ContentPart(String type, String imageUrl) {
            this.type = type;
            this.image_url = new ImageUrl(imageUrl);
        }
    }

    @Data
    public static class ImageUrl {
        private String url;

        public ImageUrl(String url) {
            this.url = url;
        }
    }
}
