package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 图表创建响应 - 单次处理版本
 */
@Data
public class ChartCreateResponse {

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
     * 是否生成成功
     */
    @JsonProperty("generation_success")
    private Boolean generationSuccess;

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
     * 使用的提示词
     */
    @JsonProperty("prompt")
    private String prompt;

    /**
     * 生成的图片URL列表
     */
    @JsonProperty("generated_image_urls")
    private List<String> generatedImageUrls;

    /**
     * 实际生成数量
     */
    @JsonProperty("generated_count")
    private Integer generatedCount;

    /**
     * 使用的模型
     */
    @JsonProperty("model")
    private String model;
}
