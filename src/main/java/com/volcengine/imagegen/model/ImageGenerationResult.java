package com.volcengine.imagegen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for image generation result
 */
public record ImageGenerationResult(
        @JsonProperty("image_url")
        String imageUrl,

        @JsonProperty("request_id")
        String requestId,

        @JsonProperty("tokens_used")
        int tokensUsed,

        @JsonProperty("status")
        String status
) {
    /**
     * Constructor with default status
     */
    public ImageGenerationResult(String imageUrl, String requestId, int tokensUsed) {
        this(imageUrl, requestId, tokensUsed, "completed");
    }
}
