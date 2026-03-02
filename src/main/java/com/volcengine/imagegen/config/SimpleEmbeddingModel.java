package com.volcengine.imagegen.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.*;

/**
 * 简单的词频向量嵌入模型（用于演示和测试）
 * 注意：这不是一个真正的语义嵌入模型，仅用于开发测试
 * 生产环境请使用专业的嵌入模型 API（如 OpenAI、VolcEngine 等）
 */
public class SimpleEmbeddingModel implements EmbeddingModel {

    private static final int VECTOR_SIZE = 384;

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        for (String text : texts) {
            result.add(generateSimpleVector(text));
        }
        return result;
    }

    @Override
    public float[] embed(String text) {
        return generateSimpleVector(text);
    }

    @Override
    public float[] embed(Document document) {
        return generateSimpleVector(document.getContent());
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        // 返回空响应，实际使用的是 embed(String) 方法
        return new EmbeddingResponse(Collections.emptyList());
    }

    @Override
    public int dimensions() {
        return VECTOR_SIZE;
    }

    /**
     * 生成简单的词频向量（仅用于演示）
     */
    private float[] generateSimpleVector(String text) {
        float[] vector = new float[VECTOR_SIZE];
        if (text == null || text.isEmpty()) {
            return vector;
        }

        // 简单的哈希+词频生成向量
        Map<Integer, Integer> wordHashes = new HashMap<>();
        String[] words = text.toLowerCase().split("\\s+");

        for (String word : words) {
            int hash = Math.abs(word.hashCode() % VECTOR_SIZE);
            wordHashes.put(hash, wordHashes.getOrDefault(hash, 0) + 1);
        }

        // 填充向量
        for (Map.Entry<Integer, Integer> entry : wordHashes.entrySet()) {
            vector[entry.getKey()] = entry.getValue();
        }

        // 归一化
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }
}
