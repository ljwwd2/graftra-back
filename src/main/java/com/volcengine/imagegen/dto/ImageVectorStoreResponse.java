package com.volcengine.imagegen.dto;

import lombok.Data;

/**
 * 存储图片向量的响应
 */
@Data
public class ImageVectorStoreResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 记录ID
     */
    private String id;

    /**
     * 消息
     */
    private String message;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 结构描述
     */
    private String structureDescription;

    public ImageVectorStoreResponse(boolean success, String id, String message, String chartType, String structureDescription) {
        this.success = success;
        this.id = id;
        this.message = message;
        this.chartType = chartType;
        this.structureDescription = structureDescription;
    }
}
