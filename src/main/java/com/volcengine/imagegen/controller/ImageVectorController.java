package com.volcengine.imagegen.controller;

import com.volcengine.imagegen.dto.*;
import com.volcengine.imagegen.model.ApiResponse;
import com.volcengine.imagegen.service.ImageVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 图片向量检索 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/image-vector")
@Tag(name = "图片向量检索", description = "基于向量相似度的图片结构检索服务")
public class ImageVectorController {

    private final ImageVectorService imageVectorService;

    public ImageVectorController(ImageVectorService imageVectorService) {
        this.imageVectorService = imageVectorService;
    }

    /**
     * 存储图片向量记录
     */
    @Operation(summary = "存储图片向量", description = "将图片的结构描述转换为向量并存储")
    @PostMapping("/store")
    public ResponseEntity<ApiResponse<ImageVectorStoreResponse>> store(
            @RequestBody ImageVectorStoreRequest request
    ) {
        try {
            ImageVectorStoreResponse response = imageVectorService.store(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to store image vector", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to store: " + e.getMessage()));
        }
    }

    /**
     * 批量存储图片向量记录
     */
    @Operation(summary = "批量存储图片向量", description = "批量将图片的结构描述转换为向量并存储")
    @PostMapping("/store/batch")
    public ResponseEntity<ApiResponse<ImageVectorBatchStoreResponse>> batchStore(
            @RequestBody List<ImageVectorStoreRequest> requests
    ) {
        try {
            ImageVectorBatchStoreResponse response = imageVectorService.batchStore(requests);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to batch store image vectors", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to batch store: " + e.getMessage()));
        }
    }

    /**
     * 搜索相似的图片
     */
    @Operation(summary = "搜索相似图片", description = "根据结构描述搜索最相似的图片，支持按图表类型过滤")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<ImageVectorSearchResponse>> search(
            @RequestBody ImageVectorSearchRequest request
    ) {
        try {
            ImageVectorSearchResponse response = imageVectorService.search(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to search image vectors", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to search: " + e.getMessage()));
        }
    }

    /**
     * 删除指定ID的记录
     */
    @Operation(summary = "删除图片向量", description = "根据ID删除指定的图片向量记录")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(
            @PathVariable String id
    ) {
        try {
            boolean deleted = imageVectorService.delete(id);
            return ResponseEntity.ok(ApiResponse.success(deleted));
        } catch (Exception e) {
            log.error("Failed to delete image vector", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete: " + e.getMessage()));
        }
    }

    /**
     * 按图表类型删除所有记录
     */
    @Operation(summary = "按类型删除", description = "删除指定图表类型的所有向量记录")
    @DeleteMapping("/chart-type/{chartType}")
    public ResponseEntity<ApiResponse<Integer>> deleteByChartType(
            @PathVariable String chartType
    ) {
        try {
            int count = imageVectorService.deleteByChartType(chartType);
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("Failed to delete by chart type", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete: " + e.getMessage()));
        }
    }

    /**
     * 获取存储统计信息
     */
    @Operation(summary = "获取统计信息", description = "获取向量存储的统计信息")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        try {
            Map<String, Object> stats = imageVectorService.getStats();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to get stats", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get stats: " + e.getMessage()));
        }
    }

    /**
     * 健康检查
     */
    @Operation(summary = "健康检查", description = "检查图片向量检索服务是否正常运行")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Image vector service is running"));
    }
}
