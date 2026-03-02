package com.volcengine.imagegen.service;

import com.volcengine.imagegen.model.ImageVectorRecord;
import com.volcengine.imagegen.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 图片向量检索服务
 * 使用内存存储和余弦相似度进行向量检索
 */
@Slf4j
@Service
public class ImageVectorService {

    /**
     * 内存存储：chartType -> (id -> ImageVectorRecord)
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, ImageVectorRecord>> vectorStore = new ConcurrentHashMap<>();

    /**
     * 嵌入模型（用于将文本转换为向量）
     * 需要配置 Spring AI 的 EmbeddingModel
     */
    private final EmbeddingModel embeddingModel;

    public ImageVectorService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 存储图片向量记录
     *
     * @param request 存储请求
     * @return 存储结果
     */
    public ImageVectorStoreResponse store(ImageVectorStoreRequest request) {
        try {
            // 1. 生成向量
            float[] embedding = generateEmbedding(request.getStructureDescription());

            // 2. 创建记录
            ImageVectorRecord record = new ImageVectorRecord(
                    request.getId() != null ? request.getId() : UUID.randomUUID().toString(),
                    request.getImageUrl(),
                    request.getChartType(),
                    request.getStructureDescription(),
                    embedding
            );
            record.setMetadata(request.getMetadata());

            // 3. 存储到对应类型的集合中
            vectorStore
                .computeIfAbsent(request.getChartType(), k -> new ConcurrentHashMap<>())
                .put(record.getId(), record);

            log.info("Stored image vector: id={}, type={}", record.getId(), request.getChartType());

            return new ImageVectorStoreResponse(
                    true,
                    record.getId(),
                    "Stored successfully",
                    record.getChartType(),
                    record.getStructureDescription()
            );

        } catch (Exception e) {
            log.error("Failed to store image vector", e);
            return new ImageVectorStoreResponse(
                    false,
                    null,
                    "Failed: " + e.getMessage(),
                    null,
                    null
            );
        }
    }

    /**
     * 批量存储图片向量记录
     *
     * @param requests 批量存储请求列表
     * @return 批量存储结果
     */
    public ImageVectorBatchStoreResponse batchStore(List<ImageVectorStoreRequest> requests) {
        List<ImageVectorStoreResponse> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (ImageVectorStoreRequest request : requests) {
            ImageVectorStoreResponse response = store(request);
            results.add(response);
            if (response.isSuccess()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        return new ImageVectorBatchStoreResponse(
                successCount == requests.size(),
                successCount,
                failCount,
                results
        );
    }

    /**
     * 检索最相似的图片
     *
     * @param request 检索请求
     * @return 检索结果列表（按相似度降序）
     */
    public ImageVectorSearchResponse search(ImageVectorSearchRequest request) {
        try {
            // 1. 生成查询向量
            float[] queryEmbedding = generateEmbedding(request.getQuery());

            // 2. 确定搜索范围
            Collection<ImageVectorRecord> searchSpace;
            if (request.getChartType() != null && !request.getChartType().isEmpty()) {
                // 按类型过滤
                ConcurrentHashMap<String, ImageVectorRecord> typeStore = vectorStore.get(request.getChartType());
                if (typeStore == null || typeStore.isEmpty()) {
                    return new ImageVectorSearchResponse(
                            true,
                            "No records found for chart type: " + request.getChartType(),
                            Collections.emptyList()
                    );
                }
                searchSpace = typeStore.values();
                log.debug("Searching in chart type: {}, records: {}", request.getChartType(), searchSpace.size());
            } else {
                // 全局搜索
                searchSpace = vectorStore.values().stream()
                        .flatMap(map -> map.values().stream())
                        .collect(Collectors.toList());
                log.debug("Searching globally, total records: {}", searchSpace.size());
            }

            // 3. 计算相似度并排序
            List<ImageVectorSearchResult> results = searchSpace.stream()
                    .map(record -> {
                        double similarity = cosineSimilarity(queryEmbedding, record.getEmbedding());
                        return new ImageVectorSearchResult(
                                record.getId(),
                                record.getImageUrl(),
                                record.getChartType(),
                                record.getStructureDescription(),
                                similarity,
                                record.getMetadata()
                        );
                    })
                    .filter(r -> r.getSimilarity() >= request.getMinScore())
                    .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                    .limit(request.getTopK() != null ? request.getTopK() : 10)
                    .collect(Collectors.toList());

            log.info("Search completed: query={}, results={}, type={}",
                    request.getQuery(), results.size(), request.getChartType());

            return new ImageVectorSearchResponse(
                    true,
                    "Search completed",
                    results
            );

        } catch (Exception e) {
            log.error("Failed to search image vectors", e);
            return new ImageVectorSearchResponse(
                    false,
                    "Search failed: " + e.getMessage(),
                    Collections.emptyList()
            );
        }
    }

    /**
     * 删除指定ID的记录
     *
     * @param id 记录ID
     * @return 是否删除成功
     */
    public boolean delete(String id) {
        for (ConcurrentHashMap<String, ImageVectorRecord> typeStore : vectorStore.values()) {
            if (typeStore.remove(id) != null) {
                log.info("Deleted image vector: id={}", id);
                return true;
            }
        }
        log.warn("Image vector not found for deletion: id={}", id);
        return false;
    }

    /**
     * 按类型删除所有记录
     *
     * @param chartType 图表类型
     * @return 删除的记录数量
     */
    public int deleteByChartType(String chartType) {
        ConcurrentHashMap<String, ImageVectorRecord> typeStore = vectorStore.remove(chartType);
        if (typeStore != null) {
            int count = typeStore.size();
            log.info("Deleted all image vectors for chart type: {}, count={}", chartType, count);
            return count;
        }
        return 0;
    }

    /**
     * 获取存储统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        int totalRecords = 0;
        Map<String, Integer> countByType = new HashMap<>();

        for (Map.Entry<String, ConcurrentHashMap<String, ImageVectorRecord>> entry : vectorStore.entrySet()) {
            int count = entry.getValue().size();
            countByType.put(entry.getKey(), count);
            totalRecords += count;
        }

        stats.put("totalRecords", totalRecords);
        stats.put("countByChartType", countByType);
        stats.put("supportedChartTypes", Arrays.asList(
                "flowchart", "sequence_diagram", "architecture_diagram", "org_chart",
                "gantt_chart", "data_flow_diagram", "state_diagram", "hierarchy_diagram",
                "module_diagram", "infographic"
        ));

        return stats;
    }

    /**
     * 生成文本的向量嵌入
     *
     * @param text 输入文本
     * @return 向量数组
     */
    private float[] generateEmbedding(String text) {
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel not configured.");
        }

        // 直接调用 embed 方法获取向量
        float[] embedding = embeddingModel.embed(text);

        return embedding;  // SimpleEmbeddingModel 已经归一化了
    }

    /**
     * 计算余弦相似度
     * （两个向量都已归一化，所以就是点积）
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度 [-1, 1]，越接近1越相似
     */
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        double dotProduct = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
        }

        return dotProduct; // 因为已归一化，点积就是余弦相似度
    }
}
