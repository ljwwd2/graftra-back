package com.volcengine.imagegen.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量嵌入模型配置
 */
@Slf4j
@Configuration
public class EmbeddingModelConfig {

    /**
     * 配置 EmbeddingModel
     * 默认使用 SimpleEmbeddingModel（基于哈希的词频向量，仅用于测试）
     *
     * 生产环境建议配置专业的嵌入服务：
     * 1. OpenAI: text-embedding-3-small, text-embedding-ada-002
     * 2. VolcEngine/Hertz: 火山引擎的嵌入API
     * 3. 阿里云通义千问: text-embedding-v2
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        // 当前默认使用 SimpleEmbeddingModel
        // 如需使用专业嵌入API，请根据Spring AI文档配置相应的EmbeddingModel实现
        log.info("Initializing SimpleEmbeddingModel (for testing only)");
        return new SimpleEmbeddingModel();
    }
}
