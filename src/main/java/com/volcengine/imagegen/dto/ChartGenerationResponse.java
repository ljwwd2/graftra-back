package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 图表生成响应
 */
@Data
public class ChartGenerationResponse {

    /**
     * 是否成功
     */
    @JsonProperty("success")
    private Boolean success;

    /**
     * 响应消息
     */
    @JsonProperty("message")
    private String message;

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

    /**
     * 生成的图表结果列表
     */
    @JsonProperty("results")
    private List<ChartGenerationResult> results;

    /**
     * 总生成数量
     */
    @JsonProperty("total_generated")
    private Integer totalGenerated;

    /**
     * 单个图表生成结果
     */
    @Data
    public static class ChartGenerationResult {

        /**
         * 图表ID
         */
        @JsonProperty("chart_id")
        private String chartId;

        /**
         * 图表类型
         */
        @JsonProperty("chart_type")
        private String chartType;

        /**
         * 是否成功
         */
        @JsonProperty("success")
        private Boolean success;

        /**
         * 错误信息（如果失败）
         */
        @JsonProperty("error_message")
        private String errorMessage;

        /**
         * 参考图URL（用于生成）
         */
        @JsonProperty("reference_image_url")
        private String referenceImageUrl;

        /**
         * 生成的图片URL列表
         */
        @JsonProperty("generated_image_urls")
        private List<String> generatedImageUrls;

        /**
         * 使用的提示词
         */
        @JsonProperty("prompt")
        private String prompt;
    }
}
