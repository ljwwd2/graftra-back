package com.volcengine.imagegen.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 图表参考图检索响应
 */
@Data
public class ChartSearchResponse {

    /**
     * 是否成功
     */
    @JsonProperty("success")
    private Boolean success;

    /**
     * 响应消息
     */
    @JsonProperty("message")
    private String message;

    /**
     * 请求ID
     */
    @JsonProperty("request_id")
    private String requestId;

    /**
     * 分析ID
     */
    @JsonProperty("analysis_id")
    private String analysisId;

    /**
     * 检索结果列表
     */
    @JsonProperty("results")
    private List<ChartSearchResult> results;

    /**
     * 单个图表检索结果
     */
    @Data
    public static class ChartSearchResult {

        /**
         * 图表ID
         */
        @JsonProperty("chart_id")
        private String chartId;

        /**
         * 图表类型
         */
        @JsonProperty("chart_type")
        private String chartType;

        /**
         * 是否成功找到参考图
         */
        @JsonProperty("success")
        private Boolean success;

        /**
         * 错误信息（如果失败）
         */
        @JsonProperty("error_message")
        private String errorMessage;

        /**
         * 检索到的参考图列表（按相似度降序）
         */
        @JsonProperty("reference_images")
        private List<ReferenceImage> referenceImages;

        /**
         * 使用的搜索查询
         */
        @JsonProperty("search_query")
        private String searchQuery;
    }

    /**
     * 参考图信息
     */
    @Data
    public static class ReferenceImage {

        /**
         * 图片ID
         */
        @JsonProperty("id")
        private String id;

        /**
         * 图片URL
         */
        @JsonProperty("image_url")
        private String imageUrl;

        /**
         * 相似度分数
         */
        @JsonProperty("similarity")
        private Double similarity;

        /**
         * 结构描述
         */
        @JsonProperty("structure_description")
        private String structureDescription;

        /**
         * 元数据
         */
        @JsonProperty("metadata")
        private String metadata;
    }
}
