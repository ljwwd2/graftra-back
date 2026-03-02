package com.volcengine.imagegen.dto;

import lombok.Data;

/**
 * 批量存储图片向量的响应
 */
@Data
public class ImageVectorBatchStoreResponse {

    /**
     * 是否全部成功
     */
    private boolean allSuccess;

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failCount;

    /**
     * 详细结果列表
     */
    private java.util.List<ImageVectorStoreResponse> results;

    public ImageVectorBatchStoreResponse(boolean allSuccess, int successCount, int failCount, java.util.List<ImageVectorStoreResponse> results) {
        this.allSuccess = allSuccess;
        this.successCount = successCount;
        this.failCount = failCount;
        this.results = results;
    }
}
