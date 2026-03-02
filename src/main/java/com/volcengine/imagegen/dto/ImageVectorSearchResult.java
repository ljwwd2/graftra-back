package com.volcengine.imagegen.dto;

import lombok.Data;

/**
 * 单个搜索结果
 */
@Data
public class ImageVectorSearchResult {

    /**
     * 记录ID
     */
    private String id;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 结构描述
     */
    private String structureDescription;

    /**
     * 相似度分数 [-1, 1]
     */
    private double similarity;

    /**
     * 元数据
     */
    private String metadata;

    public ImageVectorSearchResult() {
    }

    public ImageVectorSearchResult(String id, String imageUrl, String chartType, String structureDescription, double similarity, String metadata) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.chartType = chartType;
        this.structureDescription = structureDescription;
        this.similarity = similarity;
        this.metadata = metadata;
    }
}
