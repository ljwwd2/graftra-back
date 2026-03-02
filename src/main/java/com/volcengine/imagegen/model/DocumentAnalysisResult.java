package com.volcengine.imagegen.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for document analysis result
 */
public record DocumentAnalysisResult(
        @JsonProperty("model")
        String model,

        @JsonProperty("thinking")
        String thinking,

        @JsonProperty("response")
        String response,

        @JsonProperty("structured_data")
        ChartInfo structuredData,

        @JsonProperty("request_id")
        String requestId,

        @JsonProperty("tokens_used")
        int tokensUsed
) {
    public DocumentAnalysisResult {
        if (tokensUsed == 0) {
            tokensUsed = 0;
        }
    }
}
