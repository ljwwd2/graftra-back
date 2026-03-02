package com.volcengine.imagegen.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for image generation with file upload
 */
@Data
public class ImageGenerationUploadRequest {

    /**
     * Reference image file
     */
    private MultipartFile referenceImage;

    /**
     * Reference image URL (if image is already hosted)
     */
    private String referenceImageUrl;

    /**
     * Document file (optional)
     */
    private MultipartFile document;

    /**
     * Text content (optional, can be used instead of document)
     */
    private String textContent;

    /**
     * Prompt/instruction for the image generation
     */
    private String prompt;

    /**
     * Number of images to generate
     */
    private Integer count = 1;

    /**
     * Additional style parameters
     */
    private String style;

    /**
     * Temperature (0-1) for generation
     */
    private Double temperature = 0.7;

    /**
     * Validate the request
     */
    public boolean isValid() {
        boolean hasImage = referenceImage != null && !referenceImage.isEmpty()
                || (referenceImageUrl != null && !referenceImageUrl.isBlank());
        boolean hasContent = document != null && !document.isEmpty()
                || (textContent != null && !textContent.isBlank());

        return hasImage && hasContent && prompt != null && !prompt.isBlank();
    }
}
