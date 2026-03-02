package com.volcengine.imagegen.service;

import com.volcengine.imagegen.dto.*;
import com.volcengine.imagegen.model.OssUploadResult;
import com.volcengine.imagegen.model.SeedreamImageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图表生成服务
 * 结合向量检索和即梦生图API
 */
@Slf4j
@Service
public class ChartGenerationService {

    private final ImageVectorService imageVectorService;
    private final SeedreamImageService seedreamImageService;
    private final AliyunOssService aliyunOssService;  // Optional
    private final Random random;

    /**
     * 默认模型
     */
    private static final String DEFAULT_MODEL = "doubao-seedream-4-5-251128";

    @Autowired(required = false)
    public ChartGenerationService(ImageVectorService imageVectorService,
                                  SeedreamImageService seedreamImageService,
                                  AliyunOssService aliyunOssService) {
        this.imageVectorService = imageVectorService;
        this.seedreamImageService = seedreamImageService;
        this.aliyunOssService = aliyunOssService;  // Can be null if OSS is not configured
        this.random = new Random();
    }

    /**
     * 根据请求生成图表
     *
     * @param request 图表生成请求
     * @return 图表生成响应
     */
    public ChartGenerationResponse generateCharts(ChartGenerationRequest request) {
        // 参数验证
        if (request == null || request.getSelectedCharts() == null || request.getSelectedCharts().isEmpty()) {
            ChartGenerationResponse response = new ChartGenerationResponse();
            response.setSuccess(false);
            response.setMessage("Invalid request: selected_charts is required and cannot be empty");
            response.setResults(Collections.emptyList());
            response.setTotalGenerated(0);
            return response;
        }

        List<ChartGenerationResponse.ChartGenerationResult> results = new ArrayList<>();
        int totalGenerated = 0;

        for (ChartGenerationRequest.SelectedChart selectedChart : request.getSelectedCharts()) {
            ChartGenerationResponse.ChartGenerationResult result = generateSingleChart(selectedChart);
            results.add(result);
            if (result.getSuccess() && result.getGeneratedImageUrls() != null) {
                totalGenerated += result.getGeneratedImageUrls().size();
            }
        }

        ChartGenerationResponse response = new ChartGenerationResponse();
        response.setSuccess(true);
        response.setMessage("Chart generation completed");
        response.setResults(results);
        response.setTotalGenerated(totalGenerated);

        if (request.getDocumentContext() != null) {
            response.setRequestId(request.getDocumentContext().getRequestId());
            response.setAnalysisId(request.getDocumentContext().getAnalysisId());
        }

        log.info("Chart generation completed: total={}, requested={}", totalGenerated, request.getTotalImages());

        return response;
    }

    /**
     * 生成单个图表
     *
     * @param selectedChart 选中的图表信息
     * @return 单个图表生成结果
     */
    private ChartGenerationResponse.ChartGenerationResult generateSingleChart(
            ChartGenerationRequest.SelectedChart selectedChart) {

        ChartGenerationResponse.ChartGenerationResult result = new ChartGenerationResponse.ChartGenerationResult();
        result.setChartId(selectedChart.getChartId());
        result.setChartType(selectedChart.getChartType());
        result.setPrompt(selectedChart.getContentReplacementPrompt());

        try {
            // 1. 向量检索，找到相似的参考图
            List<ImageVectorSearchResult> similarImages = findSimilarImages(
                    selectedChart.getTemplateSearchQuery(),
                    selectedChart.getChartType()
            );

            if (similarImages.isEmpty()) {
                result.setSuccess(false);
                result.setErrorMessage("No similar reference images found for chart type: " + selectedChart.getChartType());
                log.warn("No similar images found for chart: {}, type: {}, query: {}",
                        selectedChart.getChartId(), selectedChart.getChartType(), selectedChart.getTemplateSearchQuery());
                return result;
            }

            // 2. 随机选择一张参考图
            ImageVectorSearchResult selectedReference = similarImages.get(
                    random.nextInt(similarImages.size())
            );
            result.setReferenceImageUrl(selectedReference.getImageUrl());

            log.info("Selected reference image for chart {}: {} (similarity: {})",
                    selectedChart.getChartId(), selectedReference.getImageUrl(), selectedReference.getSimilarity());

            // 3. 调用即梦生图API生成图片
            SeedreamImageUploadRequest generationRequest = new SeedreamImageUploadRequest();
            generationRequest.setReferenceImageUrl(selectedReference.getImageUrl());
            generationRequest.setPrompt(selectedChart.getContentReplacementPrompt());
            generationRequest.setModel("doubao-seedream-4-0-250828");
            generationRequest.setMaxImages(selectedChart.getGenerationCount() != null ? selectedChart.getGenerationCount() : 1);
            generationRequest.setSize("2K");
            generationRequest.setResponseFormat("url");
            generationRequest.setWatermark(false);
            generationRequest.setStream(false);

            SeedreamImageResult generationResult = seedreamImageService.generateImages(generationRequest);

            // 4. 提取生成的图片URL
            List<String> generatedUrls = generationResult.images().stream()
                    .map(SeedreamImageResult.ImageInfo::url)
                    .collect(Collectors.toList());

            result.setGeneratedImageUrls(generatedUrls);
            result.setSuccess(true);

            log.info("Generated {} images for chart: {}", generatedUrls.size(), selectedChart.getChartId());

        } catch (Exception e) {
            log.error("Failed to generate chart: {}", selectedChart.getChartId(), e);
            result.setSuccess(false);
            result.setErrorMessage("Generation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * 通过向量检索找到相似的参考图
     *
     * @param query    搜索查询
     * @param chartType 图表类型
     * @return 相似图片列表
     */
    private List<ImageVectorSearchResult> findSimilarImages(String query, String chartType) {
        ImageVectorSearchRequest searchRequest = new ImageVectorSearchRequest();
        searchRequest.setQuery(query);
        searchRequest.setChartType(chartType);
        searchRequest.setTopK(10);  // 找出最相似的10张
        searchRequest.setMinScore(0.0);  // 最低相似度阈值（0.0表示接受所有结果）

        log.info("Searching for similar images: query='{}', chartType='{}'", query, chartType);

        ImageVectorSearchResponse searchResponse = imageVectorService.search(searchRequest);

        if (!searchResponse.isSuccess()) {
            log.error("Vector search failed for query: {}, type: {}, message: {}",
                    query, chartType, searchResponse.getMessage());
            return Collections.emptyList();
        }

        if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
            log.warn("Vector search returned no results for query: {}, type: {}", query, chartType);
            return Collections.emptyList();
        }

        log.info("Found {} similar images for query: {}, type: {}, top score: {}",
                searchResponse.getResults().size(), query, chartType,
                searchResponse.getResults().get(0).getSimilarity());

        return searchResponse.getResults();
    }

    /**
     * 接口1：根据请求检索参考图
     *
     * @param request 图表生成请求
     * @return 参考图检索响应
     */
    public ChartSearchResponse searchReferenceImages(ChartGenerationRequest request) {
        // 参数验证
        if (request == null || request.getSelectedCharts() == null || request.getSelectedCharts().isEmpty()) {
            ChartSearchResponse response = new ChartSearchResponse();
            response.setSuccess(false);
            response.setMessage("Invalid request: selected_charts is required and cannot be empty");
            response.setResults(Collections.emptyList());
            return response;
        }

        List<ChartSearchResponse.ChartSearchResult> results = new ArrayList<>();

        for (ChartGenerationRequest.SelectedChart selectedChart : request.getSelectedCharts()) {
            ChartSearchResponse.ChartSearchResult result = new ChartSearchResponse.ChartSearchResult();
            result.setChartId(selectedChart.getChartId());
            result.setChartType(selectedChart.getChartType());
            result.setSearchQuery(selectedChart.getTemplateSearchQuery());

            try {
                // 向量检索，找到相似的参考图
                List<ImageVectorSearchResult> similarImages = findSimilarImages(
                        selectedChart.getTemplateSearchQuery(),
                        selectedChart.getChartType()
                );

                if (similarImages.isEmpty()) {
                    result.setSuccess(false);
                    result.setErrorMessage("No similar reference images found for chart type: " + selectedChart.getChartType());
                    result.setReferenceImages(Collections.emptyList());
                } else {
                    result.setSuccess(true);
                    // 转换为参考图列表
                    List<ChartSearchResponse.ReferenceImage> referenceImages = similarImages.stream()
                            .map(img -> {
                                ChartSearchResponse.ReferenceImage ref = new ChartSearchResponse.ReferenceImage();
                                ref.setId(img.getId());
                                ref.setImageUrl(img.getImageUrl());
                                ref.setSimilarity(img.getSimilarity());
                                ref.setStructureDescription(img.getStructureDescription());
                                ref.setMetadata(img.getMetadata());
                                return ref;
                            })
                            .collect(Collectors.toList());
                    result.setReferenceImages(referenceImages);
                }

            } catch (Exception e) {
                log.error("Failed to search reference images for chart: {}", selectedChart.getChartId(), e);
                result.setSuccess(false);
                result.setErrorMessage("Search failed: " + e.getMessage());
                result.setReferenceImages(Collections.emptyList());
            }

            results.add(result);
        }

        ChartSearchResponse response = new ChartSearchResponse();
        response.setSuccess(true);
        response.setMessage("Reference image search completed");
        response.setResults(results);

        if (request.getDocumentContext() != null) {
            response.setRequestId(request.getDocumentContext().getRequestId());
            response.setAnalysisId(request.getDocumentContext().getAnalysisId());
        }

        return response;
    }

    /**
     * 接口2：根据选定的参考图生成图片（单次处理，支持文件上传）
     *
     * @param request 图表创建请求
     * @return 图表创建响应
     */
    public ChartCreateResponse createChart(ChartCreateRequest request) {
        // 参数验证
        if (request == null || !request.isValid()) {
            ChartCreateResponse response = new ChartCreateResponse();
            response.setSuccess(false);
            response.setGenerationSuccess(false);
            response.setErrorMessage("Invalid request: reference_image (URL or file) and content_replacement_prompt are required");
            return response;
        }

        ChartCreateResponse response = new ChartCreateResponse();
        response.setChartId(request.getChartId());
        response.setChartType(request.getChartType());
        response.setPrompt(request.getContentReplacementPrompt());
        response.setRequestId(request.getRequestId());
        response.setAnalysisId(request.getAnalysisId());

        try {
            // 1. 处理参考图：上传本地文件或使用URL
            String referenceImageUrl = request.getReferenceImageUrl();
            if (request.getReferenceImageFile() != null && !request.getReferenceImageFile().isEmpty()) {
                // 上传本地文件到 OSS
                if (aliyunOssService == null) {
                    // OSS is not configured
                    response.setSuccess(false);
                    response.setGenerationSuccess(false);
                    response.setErrorMessage("File upload is not available. Please configure Aliyun OSS or use a reference image URL instead.");
                    return response;
                }
                log.info("Uploading reference image to OSS: {}", request.getReferenceImageFile().getOriginalFilename());
                OssUploadResult uploadResult = aliyunOssService.uploadFile(request.getReferenceImageFile());
                referenceImageUrl = uploadResult.fileUrl();
                log.info("Reference image uploaded: {}", referenceImageUrl);
            }
            response.setReferenceImageUrl(referenceImageUrl);

            // 2. 调用即梦生图API生成图片
            SeedreamImageUploadRequest generationRequest = new SeedreamImageUploadRequest();
            generationRequest.setReferenceImageUrl(referenceImageUrl);
            generationRequest.setPrompt(request.getContentReplacementPrompt());
            generationRequest.setModel(DEFAULT_MODEL);

            // 打印调试日志
            log.info("Setting maxImages from request.getGenerationCount() = {}", request.getGenerationCount());
            generationRequest.setMaxImages(request.getGenerationCount());

            generationRequest.setSize(request.getSize() != null ? request.getSize() : "2K");
            generationRequest.setResponseFormat("url");
            generationRequest.setWatermark(request.getWatermark() != null ? request.getWatermark() : false);
            generationRequest.setStream(false);

            log.info("Generating images for chart: {}, reference: {}, count: {}, maxImages: {}, model: {}",
                    request.getChartId(), referenceImageUrl, request.getGenerationCount(), generationRequest.getMaxImages(), DEFAULT_MODEL);

            SeedreamImageResult generationResult = seedreamImageService.generateImages(generationRequest);

            // 3. 提取生成的图片URL
            List<String> generatedUrls = generationResult.images().stream()
                    .map(SeedreamImageResult.ImageInfo::url)
                    .collect(Collectors.toList());

            response.setGeneratedImageUrls(generatedUrls);
            response.setGeneratedCount(generatedUrls.size());
            response.setGenerationSuccess(true);
            response.setSuccess(true);
            response.setMessage("Chart generation completed");
            response.setModel(DEFAULT_MODEL);

            log.info("Generated {} images for chart: {}", generatedUrls.size(), request.getChartId());

        } catch (Exception e) {
            log.error("Failed to create chart: {}", request.getChartId(), e);
            response.setSuccess(false);
            response.setGenerationSuccess(false);
            response.setErrorMessage("Creation failed: " + e.getMessage());
            response.setMessage("Chart generation failed");
        }

        return response;
    }

    /**
     * 生成图片（无参考图版本）
     *
     * @param prompt 提示词
     * @param generationCount 生成数量
     * @param size 图片尺寸
     * @param watermark 是否添加水印
     * @return 生成的图片URL列表
     */
    public ChartCreateResponse createChartWithoutReference(
            String prompt,
            Integer generationCount,
            String size,
            Boolean watermark) {

        ChartCreateResponse response = new ChartCreateResponse();
        response.setPrompt(prompt);

        try {
            // 调用即梦生图API（无参考图）
            SeedreamImageUploadRequest generationRequest = new SeedreamImageUploadRequest();
            generationRequest.setPrompt(prompt);
            generationRequest.setModel(DEFAULT_MODEL);
            generationRequest.setMaxImages(generationCount != null ? generationCount : 1);
            generationRequest.setSize(size != null ? size : "2K");
            generationRequest.setResponseFormat("url");
            generationRequest.setWatermark(watermark != null ? watermark : false);
            generationRequest.setStream(false);

            log.info("Generating images without reference: prompt='{}', count={}, model={}",
                    prompt, generationCount, DEFAULT_MODEL);

            // 传入 false 表示不需要参考图
            SeedreamImageResult generationResult = seedreamImageService.generateImages(generationRequest, false);

            // 提取生成的图片URL
            List<String> generatedUrls = generationResult.images().stream()
                    .map(SeedreamImageResult.ImageInfo::url)
                    .collect(Collectors.toList());

            response.setGeneratedImageUrls(generatedUrls);
            response.setGeneratedCount(generatedUrls.size());
            response.setGenerationSuccess(true);
            response.setSuccess(true);
            response.setMessage("Image generation completed");
            response.setModel(DEFAULT_MODEL);

            log.info("Generated {} images without reference", generatedUrls.size());

        } catch (Exception e) {
            log.error("Failed to create image without reference", e);
            response.setSuccess(false);
            response.setGenerationSuccess(false);
            response.setErrorMessage("Generation failed: " + e.getMessage());
            response.setMessage("Image generation failed");
        }

        return response;
    }
}
