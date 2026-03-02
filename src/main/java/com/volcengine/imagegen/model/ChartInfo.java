package com.volcengine.imagegen.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Model for chart information extracted from document
 */
public record ChartInfo(
        @JsonProperty("charts")
        List<Chart> charts
) {

    public ChartInfo {
        if (charts == null) {
            charts = List.of();
        }
    }

    public record Chart(
            @JsonProperty("chart_id")
            String chartId,

            @JsonProperty("chart_type")
            String chartType,

            @JsonProperty("chart_purpose")
            String chartPurpose,

            @JsonProperty("importance_score")
            Double importanceScore,

            @JsonProperty("insert_position")
            String insertPosition,

            @JsonProperty("template_search_query")
            String templateSearchQuery,

            @JsonProperty("content_replacement_prompt")
            String contentReplacementPrompt
    ) {}
}
