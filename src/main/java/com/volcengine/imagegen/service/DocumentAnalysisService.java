package com.volcengine.imagegen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.imagegen.config.PromptProperties;
import com.volcengine.imagegen.config.VolcEngineProperties;
import com.volcengine.imagegen.dto.DocumentAnalysisRequest;
import com.volcengine.imagegen.dto.ResponsesApiResponse;
import com.volcengine.imagegen.model.ChartInfo;
import com.volcengine.imagegen.model.DocumentAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * Service for document analysis
 */
@Slf4j
@Service
public class DocumentAnalysisService {

    private final VolcEngineApiService volcEngineApiService;
    private final WordToPdfConverter wordToPdfConverter;
    private final VolcEngineProperties properties;
    private final PromptProperties promptProperties;
    private final ObjectMapper objectMapper;

    public DocumentAnalysisService(VolcEngineApiService volcEngineApiService,
                                   WordToPdfConverter wordToPdfConverter,
                                   VolcEngineProperties properties,
                                   PromptProperties promptProperties,
                                   ObjectMapper objectMapper) {
        this.volcEngineApiService = volcEngineApiService;
        this.wordToPdfConverter = wordToPdfConverter;
        this.properties = properties;
        this.promptProperties = promptProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Analyze document with prompt
     * Supports PDF and Word (doc/docx) files. Word files will be automatically converted to PDF.
     *
     * @param request Analysis request
     * @return Analysis result
     * @throws Exception if analysis fails
     */
    public DocumentAnalysisResult analyze(DocumentAnalysisRequest request) throws Exception {
        // Validate request
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid request: document file is required");
        }

        MultipartFile document = request.getDocument();
        if (document == null || document.isEmpty()) {
            throw new IllegalArgumentException("Document file is required");
        }

        String originalFilename = document.getOriginalFilename();
        log.info("Analyzing document: originalName={}, size={}", originalFilename, document.getSize());

        // Validate file type
        if (!wordToPdfConverter.isPdfDocument(originalFilename) &&
            !wordToPdfConverter.isWordDocument(originalFilename)) {
            throw new IllegalArgumentException("Only PDF and Word (doc/docx) files are supported");
        }

        // Convert Word to PDF if needed
        File pdfFile;
        if (wordToPdfConverter.isWordDocument(originalFilename)) {
            log.info("Converting Word document to PDF...");
            pdfFile = wordToPdfConverter.convertToPdf(document);
            // Delete temp file after analysis
        } else {
            // Save PDF to temp file
            pdfFile = File.createTempFile("upload_", ".pdf");
            document.transferTo(pdfFile);
        }

        try {
            // Upload PDF to VolcEngine Files API
            log.info("Uploading PDF to VolcEngine Files API...");
            String fileId = volcEngineApiService.uploadFileToVolcEngine(pdfFile);
            log.info("File uploaded successfully: fileId={}", fileId);

            // Use prompt from request or fall back to default prompt from configuration file
            String effectivePrompt = request.getPrompt();
            if (effectivePrompt == null || effectivePrompt.isBlank()) {
                effectivePrompt = promptProperties.getChartExtractionPromptText();
                log.debug("Using default prompt from configuration file");
            }

            // Use configured model as default
            String model = request.getModel();
            if (model == null || model.isBlank()) {
                model = "doubao-seed-1-6-lite-251015";
            }

            // Call API using Responses API with file ID
            ResponsesApiResponse response = volcEngineApiService.analyzeDocumentWithFile(
                    fileId,
                    effectivePrompt,
                    model,
                    request.getMaxTokens()
            );

            // Check for errors
            if (response.getError() != null) {
                throw new RuntimeException("API error: " + response.getError().getMessage());
            }

            // Extract response content from output array
            // 分别提取思考过程（reasoning）和最终回答（message）
            StringBuilder thinkingContent = new StringBuilder();
            StringBuilder responseContent = new StringBuilder();

            if (response.getOutput() != null && !response.getOutput().isEmpty()) {
                for (ResponsesApiResponse.OutputItem item : response.getOutput()) {
                    if ("reasoning".equals(item.getType())) {
                        // 提取思考过程
                        if (item.getSummary() != null && !item.getSummary().isEmpty()) {
                            for (ResponsesApiResponse.SummaryItem summary : item.getSummary()) {
                                if ("summary_text".equals(summary.getType()) && summary.getText() != null) {
                                    thinkingContent.append(summary.getText());
                                }
                            }
                        }
                    } else if ("message".equals(item.getType())) {
                        // 提取最终回答
                        if (item.getContent() != null && !item.getContent().isEmpty()) {
                            for (ResponsesApiResponse.ContentItem itemContent : item.getContent()) {
                                if ("output_text".equals(itemContent.getType()) && itemContent.getText() != null) {
                                    responseContent.append(itemContent.getText());
                                }
                            }
                        }
                    }
                }
            }

            // 尝试将响应解析为结构化的图表信息
            ChartInfo structuredData = null;
            String responseStr = responseContent.toString();
            if (responseStr != null && !responseStr.isBlank()) {
                try {
                    // 尝试解析 JSON
                    structuredData = objectMapper.readValue(responseStr, ChartInfo.class);
                    log.debug("Successfully parsed chart info: {} charts",
                            structuredData.charts() != null ? structuredData.charts().size() : 0);
                } catch (Exception e) {
                    log.debug("Response is not valid JSON chart format, using as text: {}", e.getMessage());
                    // 不是 JSON 格式，structuredData 保持为 null
                }
            }

            return new DocumentAnalysisResult(
                    response.getModel(),
                    thinkingContent.toString(),
                    responseStr,
                    structuredData,
                    response.getId(),
                    response.getUsage() != null ? response.getUsage().getTotalTokens() : 0
            );

        } finally {
            // Clean up temp file
            if (pdfFile != null && pdfFile.exists()) {
                boolean deleted = pdfFile.delete();
                log.debug("Temp PDF file deleted: {}", deleted);
            }
        }
    }

    /**
     * Analyze document asynchronously with streaming
     *
     * @param request Analysis request
     * @param emitter SSE emitter for streaming responses
     */
    @Async
    public void analyzeAsync(DocumentAnalysisRequest request, SseEmitter emitter) {
        try {
            log.info("Starting async analysis");
                // Validate request
                if (!request.isValid()) {
                    sendError(emitter, "Invalid request: document file is required");
                    return;
                }

                MultipartFile document = request.getDocument();
                if (document == null || document.isEmpty()) {
                    sendError(emitter, "Document file is required");
                    return;
                }

                String originalFilename = document.getOriginalFilename();
                log.info("Analyzing document (async): originalName={}, size={}", originalFilename, document.getSize());

                // Send start event
                sendEvent(emitter, "start", "{\"status\":\"processing\",\"message\":\"Starting document analysis...\"}");

                // Validate file type
                if (!wordToPdfConverter.isPdfDocument(originalFilename) &&
                    !wordToPdfConverter.isWordDocument(originalFilename)) {
                    sendError(emitter, "Only PDF and Word (doc/docx) files are supported");
                    return;
                }

                // Convert Word to PDF if needed
                File pdfFile;
                if (wordToPdfConverter.isWordDocument(originalFilename)) {
                    sendEvent(emitter, "progress", "{\"status\":\"converting\",\"message\":\"Converting Word to PDF...\"}");
                    log.info("Converting Word document to PDF...");
                    pdfFile = wordToPdfConverter.convertToPdf(document);
                } else {
                    // Save PDF to temp file
                    pdfFile = File.createTempFile("upload_", ".pdf");
                    document.transferTo(pdfFile);
                }

                try {
                    // Upload PDF to VolcEngine Files API
                    sendEvent(emitter, "progress", "{\"status\":\"uploading\",\"message\":\"Uploading file to AI service...\"}");
                    log.info("Uploading PDF to VolcEngine Files API...");
                    String fileId = volcEngineApiService.uploadFileToVolcEngine(pdfFile);
                    log.info("File uploaded successfully: fileId={}", fileId);

                    // Use prompt from request or fall back to default prompt
                    String effectivePrompt = request.getPrompt();
                    if (effectivePrompt == null || effectivePrompt.isBlank()) {
                        effectivePrompt = promptProperties.getChartExtractionPromptText();
                    }

                    // Use configured model as default
                    String model = request.getModel();
                    if (model == null || model.isBlank()) {
                        model = "doubao-seed-1-6-lite-251015";
                    }

                    // Call API with streaming
                    sendEvent(emitter, "progress", "{\"status\":\"analyzing\",\"message\":\"AI is analyzing the document...\"}");
                    volcEngineApiService.analyzeDocumentWithFileStream(fileId, effectivePrompt, model, emitter);

                } finally {
                    // Clean up temp file
                    if (pdfFile != null && pdfFile.exists()) {
                        pdfFile.delete();
                    }
                }

            } catch (Exception e) {
                log.error("Error analyzing document", e);
                sendError(emitter, "Failed to analyze document: " + e.getMessage());
            }
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (Exception e) {
            log.error("Error sending SSE event: {}", e.getMessage());
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"success\":false,\"message\":\"" + message.replace("\"", "\\\"") + "\"}"));
            emitter.complete();
        } catch (Exception e) {
            log.error("Error sending error", e);
            emitter.completeWithError(e);
        }
    }
}
