package com.volcengine.imagegen.service;

import com.volcengine.imagegen.dto.SeedreamImageRequest;
import com.volcengine.imagegen.dto.SeedreamImageResponse;
import com.volcengine.imagegen.dto.SeedreamImageUploadRequest;
import com.volcengine.imagegen.model.SeedreamImageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Doubao Seedream image generation
 */
@Slf4j
@Service
public class SeedreamImageService {

    private final VolcEngineApiService volcEngineApiService;
    private final FileStorageService fileStorageService;

    public SeedreamImageService(VolcEngineApiService volcEngineApiService,
                                FileStorageService fileStorageService) {
        this.volcEngineApiService = volcEngineApiService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Generate images using Seedream API (with optional reference image)
     *
     * @param request The upload request
     * @param requireReferenceImage Whether reference image is required
     * @return Generated image result
     * @throws Exception if generation fails
     */
    public SeedreamImageResult generateImages(SeedreamImageUploadRequest request, boolean requireReferenceImage) throws Exception {
        // Validate request
        if (requireReferenceImage && !request.isValid()) {
            throw new IllegalArgumentException("Invalid request: reference image (file or URL) and prompt are required");
        }
        if (!requireReferenceImage && (request.getPrompt() == null || request.getPrompt().isBlank())) {
            throw new IllegalArgumentException("Invalid request: prompt is required");
        }

        // Get reference image URL (if provided)
        String referenceImageUrl = null;
        if (request.getReferenceImageUrl() != null && !request.getReferenceImageUrl().isBlank()) {
            referenceImageUrl = request.getReferenceImageUrl();
        } else if (request.getReferenceImage() != null && !request.getReferenceImage().isEmpty()) {
            MultipartFile referenceImage = request.getReferenceImage();
            String imagePath = fileStorageService.storeFileAbsolutePath(referenceImage);
            String filename = imagePath.substring(imagePath.lastIndexOf(java.io.File.separator) + 1);
            referenceImageUrl = fileStorageService.getFileUrl("/uploads/" + filename);
        }

        // Build API request
        SeedreamImageRequest apiRequest = new SeedreamImageRequest();
        apiRequest.setModel(request.getModel());
        apiRequest.setPrompt(request.getPrompt());
        if (referenceImageUrl != null) {
            apiRequest.setImage(referenceImageUrl);
        }
        apiRequest.setSequentialImageGeneration("auto");
        apiRequest.setResponseFormat(request.getResponseFormat());
        apiRequest.setSize(request.getSize());
        apiRequest.setStream(request.getStream());
        apiRequest.setWatermark(request.getWatermark());

        // Set sequential options
        SeedreamImageRequest.SequentialImageGenerationOptions options =
                new SeedreamImageRequest.SequentialImageGenerationOptions();

        log.info("Setting maxImages in SequentialImageGenerationOptions: request.getMaxImages() = {}", request.getMaxImages());
        options.setMaxImages(request.getMaxImages());
        apiRequest.setSequentialImageGenerationOptions(options);

        log.info("API request built: prompt={}, model={}, maxImages={}, hasImage={}",
                request.getPrompt(), request.getModel(), request.getMaxImages(), referenceImageUrl != null);

        // Call API
        SeedreamImageResponse response = volcEngineApiService.generateSeedreamImages(apiRequest);

        // Convert to result model
        List<SeedreamImageResult.ImageInfo> images = response.getData().stream()
                .map(data -> new SeedreamImageResult.ImageInfo(
                        data.getUrl(),
                        data.getSize()
                ))
                .collect(Collectors.toList());

        SeedreamImageResult.UsageInfo usage = null;
        if (response.getUsage() != null) {
            usage = new SeedreamImageResult.UsageInfo(
                    response.getUsage().getGeneratedImages(),
                    response.getUsage().getOutputTokens(),
                    response.getUsage().getTotalTokens()
            );
        }

        return new SeedreamImageResult(
                response.getModel(),
                response.getCreated(),
                images,
                usage
        );
    }

    /**
     * Generate images using Seedream API (reference image required)
     *
     * @param request The upload request
     * @return Generated image result
     * @throws Exception if generation fails
     */
    public SeedreamImageResult generateImages(SeedreamImageUploadRequest request) throws Exception {
        return generateImages(request, true);
    }
}
