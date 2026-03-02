package com.volcengine.imagegen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * VolcEngine API configuration properties
 */
@Data
@ConfigurationProperties(prefix = "volcengine.api")
public class VolcEngineProperties {

    /**
     * API endpoint base URL
     */
    private String endpoint;

    /**
     * Image generation endpoint path
     */
    private String imageGenerationEndpoint;

    /**
     * Model ID (e.g., doubao-seed-1-6-vision)
     */
    private String model;

    /**
     * Access Key for authentication
     */
    private String accessKey;

    /**
     * Secret Key for authentication
     */
    private String secretKey;

    /**
     * API Key for Bearer authentication
     */
    private String apiKey;

    /**
     * Region
     */
    private String region;

    /**
     * Chat/Analysis model ID
     */
    private String chatModel;

    /**
     * Get the full URL for image generation
     */
    public String getImageGenerationUrl() {
        return endpoint + imageGenerationEndpoint;
    }
}
