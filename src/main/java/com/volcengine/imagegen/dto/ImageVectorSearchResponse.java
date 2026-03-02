package com.volcengine.imagegen.dto;

import lombok.Data;

import java.util.List;

/**
 * 搜索图片向量的响应
 */
@Data
public class ImageVectorSearchResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 搜索结果列表（按相似度降序）
     */
    private List<ImageVectorSearchResult> results;

    public ImageVectorSearchResponse() {
    }

    public ImageVectorSearchResponse(boolean success, String message, List<ImageVectorSearchResult> results) {
        this.success = success;
        this.message = message;
        this.results = results;
    }
}
