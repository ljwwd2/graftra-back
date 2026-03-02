package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 图表生成请求
 */
@Data
public class ChartGenerationRequest {

    /**
     * 选中的图表列表
     */
    @JsonProperty("selected_charts")
    @NotEmpty(message = "selected_charts cannot be empty")
    @Valid
    private List<SelectedChart> selectedCharts;

    /**
     * 总图片数量
     */
    @JsonProperty("total_images")
    private Integer totalImages;

    /**
     * 文档上下文
     */
    @JsonProperty("document_context")
    private DocumentContext documentContext;

    /**
     * 选中的单个图表信息
     */
    @Data
    public static class SelectedChart {

        /**
         * 图表ID
         */
        @JsonProperty("chart_id")
        @NotBlank(message = "chart_id is required")
        private String chartId;

        /**
         * 图表类型
         */
        @JsonProperty("chart_type")
        @NotBlank(message = "chart_type is required")
        private String chartType;

        /**
         * 图表用途说明
         */
        @JsonProperty("chart_purpose")
        private String chartPurpose;

        /**
         * 重要性评分 [0-1]
         */
        @JsonProperty("importance_score")
        private Double importanceScore;

        /**
         * 插入位置
         */
        @JsonProperty("insert_position")
        private String insertPosition;

        /**
         * 模板搜索查询（用于向量检索）
         */
        @JsonProperty("template_search_query")
        @NotBlank(message = "template_search_query is required")
        private String templateSearchQuery;

        /**
         * 内容替换提示词（用于图片生成）
         */
        @JsonProperty("content_replacement_prompt")
        @NotBlank(message = "content_replacement_prompt is required")
        private String contentReplacementPrompt;

        /**
         * 生成数量
         */
        @JsonProperty("generation_count")
        private Integer generationCount;
    }

    /**
     * 文档上下文信息
     */
    @Data
    public static class DocumentContext {
        /**
         * 请求ID
         */
        @JsonProperty("request_id")
        private String requestId;

        /**
         * 分析ID
         */
        @JsonProperty("analysis_id")
        private String analysisId;
    }
}
