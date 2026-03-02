package com.volcengine.imagegen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Model for Seedream image generation result
 */
public record SeedreamImageResult(
        @JsonProperty("model")
        String model,

        @JsonProperty("created")
        Long created,

        @JsonProperty("images")
        List<ImageInfo> images,

        @JsonProperty("usage")
        UsageInfo usage
) {
    public SeedreamImageResult {
        if (images == null) {
            images = List.of();
        }
    }

    /**
     * Image information
     */
    public record ImageInfo(
            @JsonProperty("url")
            String url,

            @JsonProperty("size")
            String size
    ) {}

    /**
     * Usage information
     */
    public record UsageInfo(
            @JsonProperty("generated_images")
            Integer generatedImages,

            @JsonProperty("output_tokens")
            Integer outputTokens,

            @JsonProperty("total_tokens")
            Integer totalTokens
    ) {}
}
