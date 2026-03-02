package com.volcengine.imagegen.model;

/**
 * 图片向量检索记录
 */
public class ImageVectorRecord {

    /**
     * 唯一ID
     */
    private String id;

    /**
     * 图片URL或路径
     */
    private String imageUrl;

    /**
     * 图表类型：flowchart, sequence_diagram, architecture_diagram, org_chart,
     *            gantt_chart, data_flow_diagram, state_diagram, hierarchy_diagram,
     *            module_diagram, infographic 等
     */
    private String chartType;

    /**
     * 结构描述语句（用于生成向量）
     */
    private String structureDescription;

    /**
     * 向量表示（归一化后的浮点数组）
     */
    private float[] embedding;

    /**
     * 附加元数据（JSON格式，可选）
     */
    private String metadata;

    /**
     * 创建时间戳
     */
    private long createdAt;

    public ImageVectorRecord() {
        this.createdAt = System.currentTimeMillis();
    }

    public ImageVectorRecord(String id, String imageUrl, String chartType, String structureDescription, float[] embedding) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.chartType = chartType;
        this.structureDescription = structureDescription;
        this.embedding = embedding;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }

    public String getStructureDescription() { return structureDescription; }
    public void setStructureDescription(String structureDescription) { this.structureDescription = structureDescription; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
