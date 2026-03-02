package com.volcengine.imagegen.dto;

import lombok.Data;

/**
 * Request DTO for Doubao Seedream image generation API
 */
@Data
public class SeedreamImageRequest {

    /**
     * 模型ID
     */
    private String model;

    /**
     * 提示词
     */
    private String prompt;

    /**
     * 参考图片URL
     */
    private String image;

    /**
     * 序列图片生成模式: auto, manual
     */
    private String sequentialImageGeneration;

    /**
     * 序列图片生成选项
     */
    private SequentialImageGenerationOptions sequentialImageGenerationOptions;

    /**
     * 响应格式: url, b64_json
     */
    private String responseFormat;

    /**
     * 图片尺寸: 2K, 1080p, 720p, 512x512, 768x1024, 1024x768, 1024x1024
     */
    private String size;

    /**
     * 是否流式返回
     */
    private Boolean stream;

    /**
     * 是否添加水印
     */
    private Boolean watermark;

    @Data
    public static class SequentialImageGenerationOptions {
        /**
         * 最大生成图片数量
         */
        private Integer maxImages;
    }
}
