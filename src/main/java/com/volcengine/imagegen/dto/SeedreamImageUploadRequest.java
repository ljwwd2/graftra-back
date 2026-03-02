package com.volcengine.imagegen.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for Seedream image generation with file upload
 */
@Data
public class SeedreamImageUploadRequest {

    /**
     * 参考图片文件
     */
    private MultipartFile referenceImage;

    /**
     * 参考图片URL（如果图片已在云端）
     */
    private String referenceImageUrl;

    /**
     * 提示词
     */
    private String prompt;

    /**
     * 模型ID，默认: doubao-seedream-4-0-250828
     */
    private String model = "doubao-seedream-4-0-250828";

    /**
     * 最大生成图片数量，默认: 5
     */
    private Integer maxImages = 5;

    /**
     * 图片尺寸，默认: 2K
     * 可选: 2K, 1080p, 720p, 512x512, 768x1024, 1024x768, 1024x1024
     */
    private String size = "2K";

    /**
     * 响应格式，默认: url
     * 可选: url, b64_json
     */
    private String responseFormat = "url";

    /**
     * 是否添加水印，默认: true
     */
    private Boolean watermark = true;

    /**
     * 是否流式返回，默认: false
     */
    private Boolean stream = false;

    /**
     * 验证请求是否有效
     */
    public boolean isValid() {
        return (referenceImage != null && !referenceImage.isEmpty()
                || (referenceImageUrl != null && !referenceImageUrl.isBlank()))
                && prompt != null && !prompt.isBlank();
    }
}
