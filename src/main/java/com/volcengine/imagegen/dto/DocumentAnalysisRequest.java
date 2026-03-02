package com.volcengine.imagegen.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for document analysis
 */
@Data
public class DocumentAnalysisRequest {

    /**
     * 文档文件（PDF/DOC/DOCX）
     */
    private MultipartFile document;

    /**
     * 分析提示词（可选）
     */
    private String prompt;

    /**
     * 使用的模型（可选，不填则使用默认模型）
     */
    private String model;

    /**
     * 最大 token 数
     */
    private Integer maxTokens = 4096;

    /**
     * 验证请求是否有效
     * 仅验证文档文件是否存在
     */
    public boolean isValid() {
        return document != null && !document.isEmpty();
    }
}
