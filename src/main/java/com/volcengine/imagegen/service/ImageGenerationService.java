package com.volcengine.imagegen.service;

import com.volcengine.imagegen.dto.ImageGenerationResponse;
import com.volcengine.imagegen.dto.ImageGenerationUploadRequest;
import com.volcengine.imagegen.model.ImageGenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Main service for image generation
 */
@Slf4j
@Service
public class ImageGenerationService {

    private final VolcEngineApiService volcEngineApiService;
    private final DocumentTextExtractor documentTextExtractor;
    private final FileStorageService fileStorageService;

    public ImageGenerationService(VolcEngineApiService volcEngineApiService,
                                  DocumentTextExtractor documentTextExtractor,
                                  FileStorageService fileStorageService) {
        this.volcEngineApiService = volcEngineApiService;
        this.documentTextExtractor = documentTextExtractor;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Generate image based on reference image and document/text content
     *
     * @param request The upload request containing reference image and content
     * @return The generated image response
     * @throws Exception if generation fails
     */
    public ImageGenerationResult generateImage(ImageGenerationUploadRequest request) throws Exception {
        // Validate request
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid request: reference image, content (document or text), and prompt are required");
        }

        // Store reference image and get URL
        String referenceImageUrl = request.getReferenceImageUrl();
        if (referenceImageUrl == null || referenceImageUrl.isBlank()) {
            MultipartFile referenceImage = request.getReferenceImage();
            String imagePath = fileStorageService.storeFileAbsolutePath(referenceImage);
            referenceImageUrl = fileStorageService.getFileUrl("/uploads/" +
                    imagePath.substring(imagePath.lastIndexOf(java.io.File.separator) + 1));
        }

        // Extract text content from document or use direct text
        String textContent = request.getTextContent();
        if (textContent == null || textContent.isBlank()) {
            MultipartFile document = request.getDocument();
            if (document != null && !document.isEmpty()) {
                textContent = documentTextExtractor.extractText(document);
                log.debug("Extracted text from document: {} characters", textContent.length());
            }
        }

        // Truncate text content if too long (most APIs have limits)
        textContent = truncateText(textContent, 8000);

        // Call VolcEngine API
        ImageGenerationResponse response = volcEngineApiService.generateImage(
                referenceImageUrl,
                textContent,
                request.getPrompt()
        );

        // Check for errors
        if (response.getError() != null) {
            throw new RuntimeException("API error: " + response.getError().getMessage());
        }

        // Extract generated image URL from response
        String generatedImageUrl = null;
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            generatedImageUrl = response.getChoices().get(0).getMessage().getContent();
        }

        return new ImageGenerationResult(
                generatedImageUrl,
                response.getId(),
                response.getUsage() != null ? response.getUsage().getTotalTokens() : 0
        );
    }

    /**
     * Truncate text to maximum length
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
