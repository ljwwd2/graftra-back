package com.volcengine.imagegen.dto;

import lombok.Data;

/**
 * 存储图片向量的请求
 */
@Data
public class ImageVectorStoreRequest {

    /**
     * 唯一ID（可选，不填则自动生成UUID）
     */
    private String id;

    /**
     * 图片URL或路径
     */
    private String imageUrl;

    /**
     * 图表类型
     * flowchart, sequence_diagram, architecture_diagram, org_chart,
     * gantt_chart, data_flow_diagram, state_diagram, hierarchy_diagram,
     * module_diagram, infographic 等
     */
    private String chartType;

    /**
     * 结构描述语句（用于生成向量）
     */
    private String structureDescription;

    /**
     * 附加元数据（JSON格式，可选）
     */
    private String metadata;
}
