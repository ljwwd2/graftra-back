package com.volcengine.imagegen.controller;

import com.volcengine.imagegen.dto.ImageGenerationUploadRequest;
import com.volcengine.imagegen.dto.SeedreamImageUploadRequest;
import com.volcengine.imagegen.model.ApiResponse;
import com.volcengine.imagegen.model.ImageGenerationResult;
import com.volcengine.imagegen.model.SeedreamImageResult;
import com.volcengine.imagegen.service.ImageGenerationService;
import com.volcengine.imagegen.service.SeedreamImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
 * REST controller for image generation API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/image-generation")
@Tag(name = "图片生成", description = "基于火山引擎 Doubao Seed Vision API 的图片生成接口")
public class ImageGenerationController {

    private final ImageGenerationService imageGenerationService;
    private final SeedreamImageService seedreamImageService;

    public ImageGenerationController(ImageGenerationService imageGenerationService,
                                     SeedreamImageService seedreamImageService) {
        this.imageGenerationService = imageGenerationService;
        this.seedreamImageService = seedreamImageService;
    }

    /**
     * Generate image based on reference image and document/text content
     *
     * @param referenceImage Reference image file
     * @param referenceImageUrl URL of reference image (alternative to file upload)
     * @param document Document file containing text content
     * @param textContent Direct text content (alternative to document)
     * @param prompt Generation prompt/instruction
     * @param count Number of images to generate (default: 1)
     * @param style Style parameter for generation
     * @param temperature Temperature for generation (0-1, default: 0.7)
     * @return Generated image result
     */
    @Operation(
            summary = "生成图片",
            description = "根据参考图片和文档/文本内容生成类似风格的图片"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "图片生成成功",
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
    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageGenerationResult>> generateImage(
            @Parameter(description = "参考图片文件", required = false)
            @RequestParam(value = "referenceImage", required = false) MultipartFile referenceImage,

            @Parameter(description = "参考图片URL（与referenceImage二选一）", required = false)
            @RequestParam(value = "referenceImageUrl", required = false) String referenceImageUrl,

            @Parameter(description = "文档文件（PDF/DOC/DOCX/TXT，与textContent二选一）", required = false)
            @RequestParam(value = "document", required = false) MultipartFile document,

            @Parameter(description = "直接文本内容（与document二选一）", required = false)
            @RequestParam(value = "textContent", required = false) String textContent,

            @Parameter(description = "生成提示词/指令", required = true, example = "请根据参考图片的风格，生成一张类似的图片")
            @RequestParam(value = "prompt") String prompt,

            @Parameter(description = "生成数量", required = false, example = "1")
            @RequestParam(value = "count", required = false, defaultValue = "1") Integer count,

            @Parameter(description = "风格参数", required = false)
            @RequestParam(value = "style", required = false) String style,

            @Parameter(description = "温度参数（0-1）", required = false, example = "0.7")
            @RequestParam(value = "temperature", required = false, defaultValue = "0.7") Double temperature
    ) {
        try {
            // Build request object
            ImageGenerationUploadRequest request = new ImageGenerationUploadRequest();
            request.setReferenceImage(referenceImage);
            request.setReferenceImageUrl(referenceImageUrl);
            request.setDocument(document);
            request.setTextContent(textContent);
            request.setPrompt(prompt);
            request.setCount(count);
            request.setStyle(style);
            request.setTemperature(temperature);

            // Validate request
            if (!request.isValid()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid request: reference image, content (document or text), and prompt are required"));
            }

            // Generate image
            var result = imageGenerationService.generateImage(request);

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating image", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate image: " + e.getMessage()));
        }
    }

    /**
     * Generate images using Doubao Seedream 4.0 API
     *
     * @param referenceImage Reference image file
     * @param referenceImageUrl URL of reference image (alternative to file upload)
     * @param prompt Prompt for image generation
     * @param model Model ID (default: doubao-seedream-4-0-250828)
     * @param maxImages Maximum number of images to generate (default: 5)
     * @param size Image size (default: 2K)
     * @param responseFormat Response format: url or b64_json (default: url)
     * @param watermark Whether to add watermark (default: true)
     * @return Generated images result
     */
    @Operation(
            summary = "Seedream批量生成图片",
            description = "使用豆包Seedream 4.0模型根据参考图片批量生成多张图片"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "图片生成成功",
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
    @PostMapping(value = "/seedream/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SeedreamImageResult>> generateSeedreamImages(
            @Parameter(description = "参考图片文件", required = false)
            @RequestParam(value = "referenceImage", required = false) MultipartFile referenceImage,

            @Parameter(description = "参考图片URL（与referenceImage二选一）", required = false)
            @RequestParam(value = "referenceImageUrl", required = false) String referenceImageUrl,

            @Parameter(description = "生成提示词", required = true, example = "参考这个LOGO，做一套户外运动品牌视觉设计")
            @RequestParam(value = "prompt") String prompt,

            @Parameter(description = "模型ID", required = false, example = "doubao-seedream-4-0-250828")
            @RequestParam(value = "model", required = false, defaultValue = "doubao-seedream-4-0-250828") String model,

            @Parameter(description = "最大生成数量", required = false, example = "5")
            @RequestParam(value = "maxImages", required = false, defaultValue = "5") Integer maxImages,

            @Parameter(description = "图片尺寸", required = false, example = "2K")
            @RequestParam(value = "size", required = false, defaultValue = "2K") String size,

            @Parameter(description = "响应格式", required = false, example = "url")
            @RequestParam(value = "responseFormat", required = false, defaultValue = "url") String responseFormat,

            @Parameter(description = "是否添加水印", required = false, example = "true")
            @RequestParam(value = "watermark", required = false, defaultValue = "true") Boolean watermark
    ) {
        try {
            // Build request object
            SeedreamImageUploadRequest request = new SeedreamImageUploadRequest();
            request.setReferenceImage(referenceImage);
            request.setReferenceImageUrl(referenceImageUrl);
            request.setPrompt(prompt);
            request.setModel(model);
            request.setMaxImages(maxImages);
            request.setSize(size);
            request.setResponseFormat(responseFormat);
            request.setWatermark(watermark);

            // Validate request
            if (!request.isValid()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid request: reference image (file or URL) and prompt are required"));
            }

            // Generate images
            SeedreamImageResult result = seedreamImageService.generateImages(request);

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating images with Seedream", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate images: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Service is running"));
    }

    /**
     * API documentation endpoint
     */
    @Operation(summary = "API文档", description = "获取API文档信息")
    @GetMapping("/docs")
    public ResponseEntity<ApiResponse<ApiDocumentation>> getApiDocs() {
        ApiDocumentation docs = new ApiDocumentation(
                "VolcEngine Image Generation API",
                "1.0.0",
                "API for generating images using VolcEngine Doubao Seed Vision model",
                new Endpoint[]{
                        new Endpoint(
                                "POST",
                                "/api/v1/image-generation/generate",
                                "Generate image based on reference image and content"
                        )
                }
        );
        return ResponseEntity.ok(ApiResponse.success(docs));
    }

    /**
     * API documentation record
     */
    private record ApiDocumentation(
            String name,
            String version,
            String description,
            Endpoint[] endpoints
    ) {}

    private record Endpoint(
            String method,
            String path,
            String description
    ) {}
}
