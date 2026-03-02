package com.volcengine.imagegen.controller;

import com.volcengine.imagegen.dto.DocumentAnalysisRequest;
import com.volcengine.imagegen.model.ApiResponse;
import com.volcengine.imagegen.model.DocumentAnalysisResult;
import com.volcengine.imagegen.service.DocumentAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.servlet.http.HttpServletResponse;

/**
 * REST controller for document analysis
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/document-analysis")
@Tag(name = "文档分析", description = "文档分析接口，支持上传PDF或Word(doc/docx)文件进行分析")
public class DocumentAnalysisController {

    private final DocumentAnalysisService documentAnalysisService;

    public DocumentAnalysisController(DocumentAnalysisService documentAnalysisService) {
        this.documentAnalysisService = documentAnalysisService;
    }

    /**
     * Analyze document with AI
     *
     * @param document Document file (PDF/DOC/DOCX, required)
     * @param prompt Analysis prompt (optional, uses default chart extraction prompt if not provided)
     * @param model Model ID (optional)
     * @param maxTokens Max tokens (optional)
     * @return Analysis result
     */
    @Operation(
            summary = "分析文档",
            description = "上传PDF或Word文件，Word文件会自动转换为PDF，然后使用大模型进行分析处理"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "分析成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "请求参数错误"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "服务器内部错误"
            )
    })
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyzeDocument(
            HttpServletResponse response,
            @Parameter(description = "文档文件（PDF/DOC/DOCX，必填）", required = true)
            @RequestParam(value = "document") MultipartFile document,

            @Parameter(description = "分析提示词（可选，不填则使用默认图表提取提示词）", required = false)
            @RequestParam(value = "prompt", required = false) String prompt,

            @Parameter(description = "使用的模型（可选，不填使用doubao-seed-1-6-251015）", required = false)
            @RequestParam(value = "model", required = false) String model,

            @Parameter(description = "最大token数", required = false, example = "4096")
            @RequestParam(value = "maxTokens", required = false) Integer maxTokens
    ) {
        // Disable buffering for real-time SSE
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        // Create SSE emitter with 5 minute timeout
        SseEmitter emitter = new SseEmitter(300000L);

        // Add error and completion callbacks
        emitter.onCompletion(() -> {
            log.info("SSE emitter completed");
        });

        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out");
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"success\":false,\"message\":\"Request timed out\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Error sending timeout error", e);
            }
        });

        emitter.onError((e) -> {
            log.error("SSE emitter error: {}", e.getMessage());
        });

        // Build request object
        DocumentAnalysisRequest request = new DocumentAnalysisRequest();
        request.setDocument(document);
        request.setPrompt(prompt);
        request.setModel(model);
        request.setMaxTokens(maxTokens);

        // Validate request
        if (!request.isValid()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"success\":false,\"message\":\"Invalid request: document file is required\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Error sending validation error", e);
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Analyze document asynchronously
        log.info("Starting async document analysis");
        documentAnalysisService.analyzeAsync(request, emitter);

        return emitter;
    }

    /**
     * Health check endpoint
     */
    @Operation(summary = "健康检查", description = "检查文档分析服务是否正常运行")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Document analysis service is running"));
    }
}
