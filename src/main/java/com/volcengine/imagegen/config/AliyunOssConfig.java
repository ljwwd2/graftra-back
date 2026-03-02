package com.volcengine.imagegen.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aliyun OSS configuration
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({
        AliyunOssProperties.class,
        PromptProperties.class,
        VolcEngineProperties.class
})
public class AliyunOssConfig {

    /**
     * Create OSS client only when configuration is present and valid
     * This bean will not be created if access-key-id is not configured or empty
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(prefix = "aliyun.oss", name = "access-key-id", matchIfMissing = false)
    public OSS ossClient(AliyunOssProperties properties) {
        String accessKeyId = properties.getAccessKeyId();
        String accessKeySecret = properties.getAccessKeySecret();
        String endpoint = properties.getEndpoint();

        // Validate required properties
        if (accessKeyId == null || accessKeyId.isBlank()) {
            log.warn("Aliyun OSS access-key-id is not configured, skipping OSS client initialization");
            return null;
        }
        if (accessKeySecret == null || accessKeySecret.isBlank()) {
            log.warn("Aliyun OSS access-key-secret is not configured, skipping OSS client initialization");
            return null;
        }
        if (endpoint == null || endpoint.isBlank()) {
            log.warn("Aliyun OSS endpoint is not configured, skipping OSS client initialization");
            return null;
        }

        log.info("Initializing Aliyun OSS client with endpoint: {}", endpoint);
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
