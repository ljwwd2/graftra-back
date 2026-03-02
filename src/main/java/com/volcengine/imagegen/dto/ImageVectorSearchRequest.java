package com.volcengine.imagegen.dto;

import lombok.Data;

/**
 * 搜索图片向量的请求
 */
@Data
public class ImageVectorSearchRequest {

    /**
     * 查询的结构描述语句
     */
    private String query;

    /**
     * 图表类型过滤（可选，不填则全局搜索）
     */
    private String chartType;

    /**
     * 返回最相似的K个结果（默认10）
     */
    private Integer topK;

    /**
     * 最低相似度分数（默认0.0，范围[-1, 1]）
     */
    private Double minScore;

    public ImageVectorSearchRequest() {
        this.topK = 10;
        this.minScore = 0.0;
    }
}
