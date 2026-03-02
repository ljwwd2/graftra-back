package com.volcengine.imagegen.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.volcengine.imagegen.config.AliyunOssProperties;
import com.volcengine.imagegen.model.OssUploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

/**
 * Service for Aliyun OSS operations
 */
@Slf4j
@Service
public class AliyunOssService {

    private final OSS ossClient;
    private final AliyunOssProperties properties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public AliyunOssService(OSS ossClient, AliyunOssProperties properties) {
        this.ossClient = ossClient;
        this.properties = properties;
    }

    /**
     * Upload file to Aliyun OSS
     *
     * @param file The file to upload
     * @return Upload result with public URL
     * @throws IOException if upload fails
     */
    public OssUploadResult uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, null);
    }

    /**
     * Upload file to Aliyun OSS with custom file name
     *
     * @param file The file to upload
     * @param customFileName Custom file name (optional)
     * @return Upload result with public URL
     * @throws IOException if upload fails
     */
    public OssUploadResult uploadFile(MultipartFile file, String customFileName) throws IOException {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate unique file key
        String datePath = LocalDateTime.now().format(DATE_FORMATTER);
        String fileName = customFileName != null && !customFileName.isBlank()
                ? customFileName + extension
                : UUID.randomUUID().toString() + extension;

        String fileKey = properties.getKeyPrefix() + datePath + "/" + fileName;

        log.info("Uploading file to OSS: bucket={}, key={}, size={}",
                properties.getBucketName(), fileKey, file.getSize());

        // Set metadata
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        // Upload file
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    properties.getBucketName(),
                    fileKey,
                    inputStream,
                    metadata
            );

            PutObjectResult result = ossClient.putObject(putObjectRequest);
            log.info("File uploaded successfully: ETag={}, requestId={}",
                    result.getETag(), result.getRequestId());

        } catch (IOException e) {
            log.error("Failed to upload file to OSS", e);
            throw e;
        }

        // Generate public URL
        String fileUrl = generatePublicUrl(fileKey);

        return new OssUploadResult(
                fileName,
                fileKey,
                fileUrl,
                file.getSize(),
                file.getContentType(),
                properties.getBucketName()
        );
    }

    /**
     * Upload byte array to Aliyun OSS
     *
     * @param bytes File content as byte array
     * @param contentType Content type
     * @param fileName File name with extension
     * @return Upload result with public URL
     */
    public OssUploadResult uploadBytes(byte[] bytes, String contentType, String fileName) {
        // Generate unique file key
        String datePath = LocalDateTime.now().format(DATE_FORMATTER);
        String extension = "";
        if (fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf("."));
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
        }
        String fileKey = properties.getKeyPrefix() + datePath + "/" +
                UUID.randomUUID() + "_" + fileName + extension;

        log.info("Uploading bytes to OSS: bucket={}, key={}, size={}",
                properties.getBucketName(), fileKey, bytes.length);

        // Set metadata
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        metadata.setContentType(contentType);

        // Upload bytes
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                properties.getBucketName(),
                fileKey,
                new java.io.ByteArrayInputStream(bytes),
                metadata
        );

        PutObjectResult result = ossClient.putObject(putObjectRequest);
        log.info("Bytes uploaded successfully: ETag={}", result.getETag());

        // Generate public URL
        String fileUrl = generatePublicUrl(fileKey);

        return new OssUploadResult(
                fileName + extension,
                fileKey,
                fileUrl,
                (long) bytes.length,
                contentType,
                properties.getBucketName()
        );
    }

    /**
     * Delete file from OSS
     *
     * @param fileKey The file key to delete
     */
    public void deleteFile(String fileKey) {
        log.info("Deleting file from OSS: bucket={}, key={}",
                properties.getBucketName(), fileKey);
        ossClient.deleteObject(properties.getBucketName(), fileKey);
    }

    /**
     * Generate signed URL for the file (valid for 7 days)
     * 所有文件访问链接都需要过期时间，始终使用签名URL
     *
     * @param fileKey The file key
     * @return Signed URL valid for 7 days
     */
    private String generatePublicUrl(String fileKey) {
        // 始终使用签名 URL，有效期 7 天
        Date expiration = new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L);
        String url = ossClient.generatePresignedUrl(
                properties.getBucketName(),
                fileKey,
                expiration
        ).toString();
        log.debug("Generated signed URL for key={}, expires in 7 days", fileKey);
        return url;
    }

    /**
     * Check if bucket exists
     *
     * @return true if bucket exists
     */
    public boolean doesBucketExist() {
        return ossClient.doesBucketExist(properties.getBucketName());
    }
}
