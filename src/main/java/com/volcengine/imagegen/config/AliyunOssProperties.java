package com.volcengine.imagegen.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Aliyun OSS configuration properties
 */
@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

    /**
     * OSS 访问域名
     */
    private String endpoint;

    /**
     * Access Key ID
     */
    private String accessKeyId;

    /**
     * Access Key Secret
     */
    private String accessKeySecret;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 存储桶所在地域
     */
    private String region;

    /**
     * 自定义域名（可选，用于CDN加速）
     */
    private String customDomain;

    /**
     * 文件存储目录前缀
     */
    private String keyPrefix = "images/";
}
