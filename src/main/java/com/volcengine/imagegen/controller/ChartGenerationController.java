package com.volcengine.imagegen.controller;

import com.volcengine.imagegen.dto.*;
import com.volcengine.imagegen.model.ApiResponse;
import com.volcengine.imagegen.service.ChartGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图表生成 API
 * 结合向量检索和即梦生图API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chart-generation")
@Tag(name = "图表生成", description = "基于向量检索和即梦生图的图表生成服务")
public class ChartGenerationController {

    private final ChartGenerationService chartGenerationService;

    public ChartGenerationController(ChartGenerationService chartGenerationService) {
        this.chartGenerationService = chartGenerationService;
    }

    /**
     * 接口1：检索参考图
     */
    @Operation(
            summary = "检索参考图",
            description = """
                    根据选中的图表列表，通过向量检索找到最相似的参考图。
                    每个图表会根据template_search_query在相同chart_type中进行向量检索，
                    返回按相似度排序的参考图列表，供用户选择。
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "检索成功",
                    content = @Content(schema = @Schema(implementation = ChartSearchResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "服务器内部错误"
            )
    })
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<ChartSearchResponse>> searchReferenceImages(
            @Valid @RequestBody ChartGenerationRequest request
    ) {
        try {
            log.info("Received reference image search request: {} charts",
                    request.getSelectedCharts() != null ? request.getSelectedCharts().size() : 0);

            ChartSearchResponse response = chartGenerationService.searchReferenceImages(request);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Failed to search reference images", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to search reference images: " + e.getMessage()));
        }
    }

    /**
     * 接口2：根据参考图生成图片（单次处理，支持文件上传）
     */
    @Operation(
            summary = "生成图片",
            description = """
                    根据用户选定的参考图和提示词生成新的图片。
                    支持本地文件上传，会自动上传到OSS获取公网链接后再调用即梦API。
                    单次处理一个图表，前端可并发调用多个请求。
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "生成成功",
                    content = @Content(schema = @Schema(implementation = ChartCreateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "服务器内部错误"
            )
    })
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChartCreateResponse>> createChart(
            @Parameter(description = "图表ID", required = true)
            @RequestParam("chart_id") String chartId,

            @Parameter(description = "图表类型", required = true)
            @RequestParam("chart_type") String chartType,

            @Parameter(description = "参考图URL（与reference_image_file二选一）", required = false)
            @RequestParam(value = "reference_image_url", required = false) String referenceImageUrl,

            @Parameter(description = "参考图文件（与reference_image_url二选一）", required = false)
            @RequestParam(value = "reference_image_file", required = false) MultipartFile referenceImageFile,

            @Parameter(description = "内容替换提示词", required = true)
            @RequestParam("content_replacement_prompt") String contentReplacementPrompt,

            @Parameter(description = "生成数量", required = true)
            @RequestParam("generation_count") Integer generationCount,

            @Parameter(description = "图片尺寸（可选，默认2K）", required = false)
            @RequestParam(value = "size", required = false, defaultValue = "2K") String size,

            @Parameter(description = "是否添加水印（可选，默认false）", required = false)
            @RequestParam(value = "watermark", required = false, defaultValue = "false") Boolean watermark,

            @Parameter(description = "请求ID（可选）", required = false)
            @RequestParam(value = "request_id", required = false) String requestId,

            @Parameter(description = "分析ID（可选）", required = false)
            @RequestParam(value = "analysis_id", required = false) String analysisId
    ) {
        try {
            log.info("Received chart creation request: chartId={}, type={}, hasReferenceFile={}",
                    chartId, chartType, referenceImageFile != null && !referenceImageFile.isEmpty());

            // 构建请求对象
            ChartCreateRequest request = new ChartCreateRequest();
            request.setChartId(chartId);
            request.setChartType(chartType);
            request.setReferenceImageUrl(referenceImageUrl);
            request.setReferenceImageFile(referenceImageFile);
            request.setContentReplacementPrompt(contentReplacementPrompt);
            request.setGenerationCount(generationCount);
            request.setSize(size);
            request.setWatermark(watermark);
            request.setRequestId(requestId);
            request.setAnalysisId(analysisId);

            ChartCreateResponse response = chartGenerationService.createChart(request);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Failed to create chart", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to create chart: " + e.getMessage()));
        }
    }

    /**
     * 接口2b：无参考图生成图片
     */
    @Operation(
            summary = "无参考图生成",
            description = "直接根据提示词生成图片，无需参考图。"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "生成成功",
                    content = @Content(schema = @Schema(implementation = ChartCreateResponse.class))
            )
    })
    @PostMapping(value = "/create-no-reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChartCreateResponse>> createChartNoReference(
            @Parameter(description = "提示词", required = true)
            @RequestParam("prompt") String prompt,

            @Parameter(description = "生成数量", required = true)
            @RequestParam("generation_count") Integer generationCount,

            @Parameter(description = "图片尺寸（可选，默认2K）", required = false)
            @RequestParam(value = "size", required = false, defaultValue = "2K") String size,

            @Parameter(description = "是否添加水印（可选，默认false）", required = false)
            @RequestParam(value = "watermark", required = false, defaultValue = "false") Boolean watermark
    ) {
        try {
            log.info("Received no-reference generation request: prompt='{}', count={}", prompt, generationCount);

            ChartCreateResponse response = chartGenerationService.createChartWithoutReference(
                    prompt, generationCount, size, watermark);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("Failed to create image without reference", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to create image: " + e.getMessage()));
        }
    }

    /**
     * 健康检查
     */
    @Operation(summary = "健康检查", description = "检查图表生成服务是否正常运行")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Chart generation service is running"));
    }
}
