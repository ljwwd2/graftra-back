package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图表创建请求（根据选定的参考图生成图片）- 单次处理版本
 */
@Data
public class ChartCreateRequest {

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
     * 参考图URL（从第一个接口返回的结果中选择）
     * 如果提供了 referenceImageFile，则此字段可选
     */
    @JsonProperty("reference_image_url")
    private String referenceImageUrl;

    /**
     * 参考图文件（本地上传）
     * 如果提供了 referenceImageUrl，则此字段可选
     */
    private MultipartFile referenceImageFile;

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
    @NotNull(message = "generation_count is required")
    private Integer generationCount;

    /**
     * 图片尺寸（可选，默认2K）
     * 可选: 2K, 1080p, 720p, 512x512, 768x1024, 1024x768, 1024x1024
     */
    @JsonProperty("size")
    private String size = "2K";

    /**
     * 是否添加水印（可选，默认false）
     */
    @JsonProperty("watermark")
    private Boolean watermark = false;

    /**
     * 请求ID（可选）
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * 分析ID（可选）
     */
    @JsonProperty("analysis_id")
    private String analysisId;

    /**
     * 验证请求是否有效
     * 参考图URL和参考图文件至少提供一个
     */
    public boolean isValid() {
        boolean hasReferenceImage = (referenceImageUrl != null && !referenceImageUrl.isBlank())
                || (referenceImageFile != null && !referenceImageFile.isEmpty());
        return hasReferenceImage
                && contentReplacementPrompt != null && !contentReplacementPrompt.isBlank()
                && generationCount != null && generationCount > 0;
    }
}
