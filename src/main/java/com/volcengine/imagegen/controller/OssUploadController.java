package com.volcengine.imagegen.controller;

import com.volcengine.imagegen.model.ApiResponse;
import com.volcengine.imagegen.model.OssUploadResult;
import com.volcengine.imagegen.service.AliyunOssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for Aliyun OSS upload
 * Only created when OSS service is available
 */
@Slf4j
@RestController
@RequestMapping("/api/oss")
@ConditionalOnProperty(prefix = "aliyun.oss", name = "enabled", havingValue = "true", matchIfMissing = false)
@Tag(name = "阿里云OSS", description = "阿里云OSS文件上传接口（支持任意文件类型，返回7天有效期的签名URL）")
public class OssUploadController {

    private final AliyunOssService aliyunOssService;

    @Autowired(required = false)
    public OssUploadController(AliyunOssService aliyunOssService) {
        this.aliyunOssService = aliyunOssService;
    }

    /**
     * Upload file to Aliyun OSS
     *
     * @param file The file to upload (image, word, pdf, etc.)
     * @param fileName Custom file name (optional)
     * @return Upload result with signed URL (valid for 7 days)
     */
    @Operation(
            summary = "上传文件到OSS",
            description = "支持上传任意类型文件（图片、Word、PDF等），返回带签名URL（7天有效期）"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "上传成功",
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
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OssUploadResult>> uploadFile(
            @Parameter(description = "文件（支持图片、Word、PDF等任意类型）", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "自定义文件名（可选，不含后缀名）", required = false)
            @RequestParam(value = "fileName", required = false) String fileName
    ) {
        try {
            // Validate file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File cannot be empty"));
            }

            // Validate file size (max 50MB for documents)
            long maxSize = 50 * 1024 * 1024; // 50MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File size exceeds 50MB limit"));
            }

            // Get file extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File must have an extension"));
            }

            log.info("Uploading file: originalName={}, size={}, contentType={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            // Upload to OSS
            OssUploadResult result = aliyunOssService.uploadFile(file, fileName);

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading file to OSS", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Check OSS connection status
     */
    @Operation(summary = "检查OSS连接", description = "检查OSS服务连接状态")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<OssHealthInfo>> checkHealth() {
        boolean bucketExists = aliyunOssService.doesBucketExist();
        OssHealthInfo healthInfo = new OssHealthInfo(
                bucketExists ? "connected" : "disconnected",
                bucketExists,
                bucketExists ? "OSS bucket is accessible" : "OSS bucket is not accessible"
        );
        return ResponseEntity.ok(ApiResponse.success(healthInfo));
    }

    /**
     * OSS health info record
     */
    private record OssHealthInfo(
            String status,
            boolean bucketExists,
            String message
    ) {}
}
