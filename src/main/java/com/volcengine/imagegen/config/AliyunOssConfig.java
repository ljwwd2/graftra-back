package com.volcengine.imagegen.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Aliyun OSS configuration
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({AliyunOssProperties.class, PromptProperties.class, VolcEngineProperties.class})
public class AliyunOssConfig {

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(AliyunOssProperties properties) {
        log.info("Initializing Aliyun OSS client with endpoint: {}", properties.getEndpoint());
        return new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
    }
}
