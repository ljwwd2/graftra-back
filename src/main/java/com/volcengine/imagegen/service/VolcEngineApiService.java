package com.volcengine.imagegen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.imagegen.config.VolcEngineProperties;
import com.volcengine.imagegen.dto.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service for calling VolcEngine API
 */
@Slf4j
@Service
public class VolcEngineApiService {

    private final OkHttpClient httpClient;
    private final VolcEngineProperties properties;
    private final ObjectMapper objectMapper;

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    public VolcEngineApiService(OkHttpClient httpClient,
                                VolcEngineProperties properties,
                                ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Generate image using reference image and text content
     *
     * @param referenceImageUrl URL of the reference image
     * @paramtextContent        Text content from document or direct input
     * @param prompt             Additional prompt for generation
     * @return Generated image URL or content
     * @throws Exception if API call fails
     */
    /**
     * Generate image using reference image and text content
     *
     * @param referenceImageUrl URL of the reference image
     * @param textContent       Text content from document or direct input
     * @param prompt            Additional prompt for generation
     * @return Generated image URL or content
     * @throws Exception if API call fails
     */
    public ImageGenerationResponse generateImage(String referenceImageUrl,
                                                  String textContent,
                                                  String prompt) throws Exception {
        // Build the request based on VolcEngine API format
        ImageGenerationRequest request = buildRequest(referenceImageUrl, textContent, prompt);

        String jsonRequest = objectMapper.writeValueAsString(request);
        log.debug("Request payload: {}", jsonRequest);

        RequestBody body = RequestBody.create(
                jsonRequest,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request httpRequest = new Request.Builder()
                .url(properties.getImageGenerationUrl())
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("API call failed with status {}: {}", response.code(), errorBody);
                throw new RuntimeException("API call failed: " + errorBody);
            }

            String responseBody = response.body().string();
            log.debug("Response: {}", responseBody);

            return objectMapper.readValue(responseBody, ImageGenerationResponse.class);
        }
    }

    /**
     * Generate images using Doubao Seedream 4.0 API
     *
     * @param request The Seedream image generation request
     * @return Generated image response
     * @throws Exception if API call fails
     */
    public SeedreamImageResponse generateSeedreamImages(SeedreamImageRequest request) throws Exception {
        String jsonRequest = objectMapper.writeValueAsString(request);
        log.info("Seedream request payload: {}", jsonRequest);

        RequestBody body = RequestBody.create(
                jsonRequest,
                MediaType.parse("application/json; charset=utf-8")
        );

        String url = properties.getEndpoint() + "/images/generations";
        Request httpRequest = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Seedream API call failed with status {}: {}", response.code(), errorBody);
                throw new RuntimeException("Seedream API call failed: " + errorBody);
            }

            String responseBody = response.body().string();
            log.info("Seedream response: {}", responseBody);

            return objectMapper.readValue(responseBody, SeedreamImageResponse.class);
        }
    }

    /**
     * Analyze document using VolcEngine chat completions API
     *
     * @param content Document content or text
     * @param prompt Analysis prompt
     * @param model Model ID
     * @param temperature Temperature parameter
     * @param maxTokens Maximum tokens
     * @return Analysis response
     * @throws Exception if API call fails
     */
    public DocumentAnalysisResponse analyzeDocument(String content, String prompt,
                                                      String model, Double temperature, Integer maxTokens) throws Exception {
        // Build chat completion request
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(model != null ? model : "doubao-pro-4k-32k");
        request.setTemperature(temperature != null ? temperature : 0.7);
        request.setMaxTokens(maxTokens != null ? maxTokens : 4096);
        request.setTopP(0.9);
        request.setStream(false);

        // Build messages
        List<ChatCompletionRequest.Message> messages = new ArrayList<>();

        // System message
        messages.add(new ChatCompletionRequest.Message("system",
                "你是一个专业的文档分析助手，能够根据用户提供的内容和提示词进行深入分析。"));

        // User message with content and prompt
        String userMessage = String.format("""
                【文档内容】
                %s

                【分析要求】
                %s

                请根据以上内容进行分析回答。
                """, content, prompt);
        messages.add(new ChatCompletionRequest.Message("user", userMessage));

        request.setMessages(messages);

        String jsonRequest = objectMapper.writeValueAsString(request);
        log.debug("Document analysis request: {}", jsonRequest);

        RequestBody body = RequestBody.create(
                jsonRequest,
                MediaType.parse("application/json; charset=utf-8")
        );

        // Use chat completions endpoint
        String url = properties.getEndpoint() + "/chat/completions";
        Request httpRequest = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Document analysis API call failed with status {}: {}", response.code(), errorBody);
                throw new RuntimeException("Document analysis API call failed: " + errorBody);
            }

            String responseBody = response.body().string();
            log.debug("Document analysis response: {}", responseBody);

            return objectMapper.readValue(responseBody, DocumentAnalysisResponse.class);
        }
    }

    /**
     * Analyze document using VolcEngine Responses API
     *
     * @param content Document content or text
     * @param prompt Analysis prompt
     * @param model Model ID
     * @param maxTokens Maximum tokens
     * @return Analysis response
     * @throws Exception if API call fails
     */
    public ResponsesApiResponse analyzeDocumentWithResponsesApi(String content, String prompt, String model, Integer maxTokens) throws Exception {
        // Build Responses API request
        ResponsesApiRequest request = new ResponsesApiRequest();
        request.setModel(model != null ? model : "doubao-seed-2-0-pro-260215");

        // Build input messages
        List<ResponsesApiRequest.InputMessage> input = new ArrayList<>();

        // User message with content and prompt
        List<ResponsesApiRequest.ContentItem> contentItems = new ArrayList<>();

        // Add text content
        contentItems.add(new ResponsesApiRequest.ContentItem("input_text",
                "【文档内容】\n" + content + "\n\n【分析要求】\n" + prompt));

        input.add(new ResponsesApiRequest.InputMessage());
        input.get(0).setRole("user");
        input.get(0).setContent(contentItems);

        request.setInput(input);

        String jsonRequest = objectMapper.writeValueAsString(request);
        log.debug("Responses API request: {}", jsonRequest);

        RequestBody body = RequestBody.create(
                jsonRequest,
                MediaType.parse("application/json; charset=utf-8")
        );

        // Use responses endpoint
        String url = properties.getEndpoint() + "/responses";
        Request httpRequest = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Responses API call failed with status {}: {}", response.code(), errorBody);
                throw new RuntimeException("Responses API call failed: " + errorBody);
            }

            String responseBody = response.body().string();
            log.debug("Responses API response: {}", responseBody);

            return objectMapper.readValue(responseBody, ResponsesApiResponse.class);
        }
    }

    /**
     * Build the API request object
     */
    private ImageGenerationRequest buildRequest(String referenceImageUrl,
                                                 String textContent,
                                                 String prompt) {
        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setModel(properties.getModel());
        request.setTemperature(0.7);
        request.setTopP(0.9);
        request.setMaxTokens(2048);
        request.setStream(false);

        // Build messages with vision content
        List<ImageGenerationRequest.Message> messages = new ArrayList<>();

        // Create content parts with text and image
        List<ImageGenerationRequest.ContentPart> parts = new ArrayList<>();

        // Add reference image
        if (referenceImageUrl != null && !referenceImageUrl.isBlank()) {
            parts.add(new ImageGenerationRequest.ContentPart("image_url", referenceImageUrl));
        }

        // Add text content and prompt
        String fullPrompt = buildFullPrompt(textContent, prompt);
        parts.add(new ImageGenerationRequest.ContentPart(fullPrompt));

        ImageGenerationRequest.Content content = new ImageGenerationRequest.Content(parts);
        messages.add(new ImageGenerationRequest.Message("user", content));

        request.setMessages(messages);

        return request;
    }

    /**
     * Build the full prompt combining content and user prompt
     */
    private String buildFullPrompt(String textContent, String prompt) {
        StringBuilder fullPrompt = new StringBuilder();
        if (textContent != null && !textContent.isBlank()) {
            fullPrompt.append("参考文档内容：\n").append(textContent).append("\n\n");
        }
        if (prompt != null && !prompt.isBlank()) {
            fullPrompt.append("生成要求：\n").append(prompt);
        }
        // Default prompt for image generation based on reference
        if (fullPrompt.length() == 0) {
            fullPrompt.append("请根据参考图片的风格和内容，生成一张类似的图片。");
        } else {
            fullPrompt.append("\n\n请根据以上信息和参考图片，生成一张符合要求的图片。");
        }
        return fullPrompt.toString();
    }

    /**
     * Calculate signature for VolcEngine API authentication (if using signature method)
     * This is a placeholder implementation - adjust based on actual API requirements
     */
    private String calculateSignature(String timestamp, String method, String path, String query, String body) throws Exception {
        // VolcEngine signature calculation
        // This is a simplified version - please adjust based on actual API documentation

        String canonicalRequest = method + "\n" + path + "\n" + query + "\n" +
                "content-type:application/json\n" +
                "host:" + properties.getEndpoint().replace("https://", "") + "\n" +
                "x-date:" + timestamp + "\n";

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(body.getBytes(StandardCharsets.UTF_8));
        String hashedPayload = bytesToHex(md.digest());

        canonicalRequest += "\n" + hashedPayload;

        md.reset();
        md.update(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        String hashedCanonicalRequest = bytesToHex(md.digest());

        String stringToSign = "HMAC-SHA256\n" + timestamp + "\n" + hashedCanonicalRequest;

        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                properties.getSecretKey().getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM
        );
        mac.init(secretKeySpec);
        byte[] signature = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(signature);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Upload file to VolcEngine Files API
     *
     * @param pdfFile The PDF file to upload
     * @return File ID from VolcEngine
     * @throws Exception if upload fails
     */
    public String uploadFileToVolcEngine(java.io.File pdfFile) throws Exception {
        log.info("Uploading PDF to VolcEngine: file={}, size={}", pdfFile.getName(), pdfFile.length());

        // Create multipart request body
        okhttp3.MultipartBody requestBody = new okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("purpose", "user_data")
                .addFormDataPart("file", pdfFile.getName(),
                        RequestBody.create(pdfFile, MediaType.parse("application/pdf")))
                .build();

        Request httpRequest = new Request.Builder()
                .url(properties.getEndpoint() + "/files")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("VolcEngine file upload failed with status {}: {}", response.code(), errorBody);
                throw new RuntimeException("File upload failed: " + errorBody);
            }

            String responseBody = response.body().string();
            log.debug("VolcEngine file upload response: {}", responseBody);

            VolcEngineFileApiResponse fileResponse = objectMapper.readValue(responseBody, VolcEngineFileApiResponse.class);

            if (fileResponse.getError() != null) {
                throw new RuntimeException("File upload error: " + fileResponse.getError().getMessage());
            }

            log.info("File uploaded successfully: fileId={}, filename={}", fileResponse.getId(), fileResponse.getFilename());
            return fileResponse.getId();
        }
    }

    /**
     * Wait for file to be ready for processing
     * Polls the file status until it's no longer in 'processing' state
     *
     * @param fileId The file ID to check
     * @throws Exception if file status check fails or times out
     */
    private void waitForFileReady(String fileId) throws Exception {
        log.info("Waiting for file to be ready: fileId={}", fileId);

        int maxAttempts = 30;  // Maximum 30 attempts
        int intervalMs = 2000;  // Wait 2 seconds between attempts

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Check file status
            Request httpRequest = new Request.Builder()
                    .url(properties.getEndpoint() + "/files/" + fileId)
                    .get()
                    .addHeader("Authorization", "Bearer " + properties.getApiKey())
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("Failed to check file status: {}", errorBody);
                    throw new RuntimeException("Failed to check file status: " + errorBody);
                }

                String responseBody = response.body().string();
                VolcEngineFileApiResponse fileResponse = objectMapper.readValue(responseBody, VolcEngineFileApiResponse.class);

                if (fileResponse.getError() != null) {
                    throw new RuntimeException("File status check error: " + fileResponse.getError().getMessage());
                }

                String status = fileResponse.getStatus();
                log.debug("File status: fileId={}, status={}, attempt={}", fileId, status, attempt + 1);

                // Check if file is ready (not in 'processing' state)
                if (!"processing".equals(status)) {
                    log.info("File is ready: fileId={}, status={}", fileId, status);
                    return;
                }

                // Wait before next attempt
                if (attempt < maxAttempts - 1) {
                    Thread.sleep(intervalMs);
                }
            }
        }

        throw new RuntimeException("File processing timeout: fileId=" + fileId + " - file is still in 'processing' state after " + (maxAttempts * intervalMs / 1000) + " seconds");
    }

    /**
     * Analyze document using VolcEngine Responses API with file ID
     *
     * @param fileId The file ID from VolcEngine Files API
     * @param prompt Analysis prompt
     * @param model Model ID
     * @param maxTokens Maximum tokens
     * @return Analysis response
     * @throws Exception if API call fails
     */
    public ResponsesApiResponse analyzeDocumentWithFile(String fileId, String prompt, String model, Integer maxTokens) throws Exception {
        log.info("Analyzing document with file: fileId={}, model={}", fileId, model);

        // Wait for file to be ready (not in 'processing' state)
        waitForFileReady(fileId);

        // Build Responses API request
        ResponsesApiRequest request = new ResponsesApiRequest();
        request.setModel(model != null ? model : "doubao-seed-1-6-lite-251015");
        // Note: max_tokens parameter removed as Responses API doesn't support it

        // Build input messages
        List<ResponsesApiRequest.InputMessage> input = new ArrayList<>();

        // User message with file and prompt
        List<ResponsesApiRequest.ContentItem> contentItems = new ArrayList<>();

        // Add file reference
        ResponsesApiRequest.ContentItem fileItem = new ResponsesApiRequest.ContentItem("input_file", fileId, true, false);
        contentItems.add(fileItem);

        // Add prompt text
        ResponsesApiRequest.ContentItem textItem = new ResponsesApiRequest.ContentItem("input_text", prompt);
        contentItems.add(textItem);

        ResponsesApiRequest.InputMessage message = new ResponsesApiRequest.InputMessage();
        message.setRole("user");
        message.setContent(contentItems);
        input.add(message);

        request.setInput(input);

        String jsonRequest = objectMapper.writeValueAsString(request);
        log.debug("Responses API request with file: {}", jsonRequest);

        RequestBody body = RequestBody.create(
                jsonRequest,
                MediaType.parse("application/json; charset=utf-8")
        );

        // Use responses endpoint
        String url = properties.getEndpoint() + "/responses";
        Request httpRequest = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Responses API call failed with status {}: {}", response.code(), errorBody);
                throw new RuntimeException("Responses API call failed: " + errorBody);
            }

            String responseBody = response.body().string();
            log.debug("Responses API response: {}", responseBody);

            return objectMapper.readValue(responseBody, ResponsesApiResponse.class);
        }
    }

    /**
     * Analyze document using VolcEngine Responses API with streaming
     *
     * @param fileId The file ID from VolcEngine Files API
     * @param prompt Analysis prompt
     * @param model Model ID
     * @param emitter SSE emitter for streaming responses
     * @throws Exception if API call fails
     */
    public void analyzeDocumentWithFileStream(String fileId, String prompt, String model, SseEmitter emitter) throws Exception {
        log.info("Analyzing document with streaming: fileId={}, model={}", fileId, model);

        // Wait for file to be ready
        waitForFileReady(fileId);

        // Build Responses API request with stream enabled
        ResponsesApiRequest request = new ResponsesApiRequest();
        request.setModel(model != null ? model : "doubao-seed-1-6-lite-251015");
        request.setStream(true);  // Enable streaming

        // Build input messages
        List<ResponsesApiRequest.InputMessage> input = new ArrayList<>();

        // User message with file and prompt
        List<ResponsesApiRequest.ContentItem> contentItems = new ArrayList<>();

        // Add file reference
        ResponsesApiRequest.ContentItem fileItem = new ResponsesApiRequest.ContentItem("input_file", fileId, true, false);
        contentItems.add(fileItem);

        // Add prompt text
        ResponsesApiRequest.ContentItem textItem = new ResponsesApiRequest.ContentItem("input_text", prompt);
        contentItems.add(textItem);

        ResponsesApiRequest.InputMessage message = new ResponsesApiRequest.InputMessage();
        message.setRole("user");
        message.setContent(contentItems);
        input.add(message);

        request.setInput(input);

        String jsonRequest = objectMapper.writeValueAsString(request);
        log.debug("Responses API streaming request: {}", jsonRequest);

        RequestBody body = RequestBody.create(
                jsonRequest,
                MediaType.parse("application/json; charset=utf-8")
        );

        // Use responses endpoint with streaming
        String url = properties.getEndpoint() + "/responses";
        Request httpRequest = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + properties.getApiKey())
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Responses API streaming call failed with status {}: {}", response.code(), errorBody);
                throw new RuntimeException("Responses API call failed: " + errorBody);
            }

            log.info("Starting to read streaming response... Content-Type: {}",
                    response.header("Content-Type"));

            // Read streaming response with minimal buffer for real-time processing
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(response.body().byteStream(), java.nio.charset.StandardCharsets.UTF_8),
                    512  // Small buffer size (512 bytes) for lower latency
            );

            String line;
            StringBuilder thinkingContent = new StringBuilder();
            StringBuilder responseContent = new StringBuilder();
            String responseId = null;
            String responseModel = null;
            int totalTokens = 0;
            int chunkCount = 0;

            // Check if response is actually streaming (Content-Type should contain text/event-stream)
            boolean isStreaming = response.header("Content-Type") != null &&
                    response.header("Content-Type").contains("text/event-stream");
            log.info("Is streaming response: {}", isStreaming);

            // Send chunk event
            try {
                emitter.send(SseEmitter.event().name("chunk").data("{\"type\":\"start\",\"message\":\"Analysis started\"}"));
            } catch (Exception e) {
                log.error("Error sending start event: {}", e.getMessage());
            }

            // For non-streaming or single JSON response, accumulate entire response
            StringBuilder fullResponseBuilder = new StringBuilder();

            // Read response line by line
            while ((line = reader.readLine()) != null) {
                log.debug("Received line: {}", line);

                // If not streaming, accumulate all lines for later parsing
                if (!isStreaming) {
                    fullResponseBuilder.append(line);
                    continue;
                }

                // For streaming, parse each line
                // Try different formats
                String data = null;

                if (line.startsWith("data: ")) {
                    // SSE format
                    data = line.substring(6).trim();
                    log.debug("SSE format data: {}", data);
                } else if (line.trim().startsWith("{") || line.trim().startsWith("[")) {
                    // Raw JSON format
                    data = line.trim();
                    log.debug("Raw JSON format data: {}", data);
                } else if (!line.trim().isEmpty()) {
                    // Other non-empty lines - accumulate
                    fullResponseBuilder.append(line);
                    log.debug("Accumulating data: {}", line);
                    continue;
                }

                if (data == null || data.isEmpty()) {
                    continue;
                }

                if ("[DONE]".equals(data)) {
                    log.info("Received [DONE] signal");
                    break;
                }

                try {
                    // Parse JSON data
                    com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(data);
                    chunkCount++;

                    String eventType = jsonNode.has("type") ? jsonNode.get("type").asText() : "";
                    log.debug("Parsed chunk #{}, type: {}", chunkCount, eventType);

                    // Extract metadata from any chunk
                    if (jsonNode.has("id")) {
                        responseId = jsonNode.get("id").asText();
                    }
                    if (jsonNode.has("model")) {
                        responseModel = jsonNode.get("model").asText();
                    }
                    if (jsonNode.has("usage")) {
                        com.fasterxml.jackson.databind.JsonNode usage = jsonNode.get("usage");
                        if (usage.has("total_tokens")) {
                            totalTokens = usage.get("total_tokens").asInt();
                        }
                    }

                    // Handle different event types from VolcEngine streaming API
                    switch (eventType) {
                        case "response.reasoning.delta":
                        case "response.reasoning_summary_text.delta":
                            // Thinking process delta - stream to client
                            if (jsonNode.has("delta")) {
                                String delta = jsonNode.get("delta").asText();
                                thinkingContent.append(delta);
                                log.info("Thinking delta [{}]: {}", eventType, delta.substring(0, Math.min(100, delta.length())));

                                // Send thinking chunk to client
                                try {
                                    emitter.send(SseEmitter.event().name("thinking")
                                            .data("{\"type\":\"thinking\",\"text\":\"" +
                                                    escapeJson(delta) + "\"}"));
                                } catch (Exception e) {
                                    log.error("Error sending thinking event: {}", e.getMessage());
                                }
                            }
                            break;

                        case "response.reasoning.done":
                        case "response.reasoning_summary_text.done":
                            log.info("Thinking process completed");
                            break;

                        case "response.output_text.delta":
                            // Output text delta - stream to client
                            if (jsonNode.has("delta")) {
                                String delta = jsonNode.get("delta").asText();
                                responseContent.append(delta);
                                log.debug("Content delta: {}", delta.substring(0, Math.min(50, delta.length())));

                                // Send content chunk to client
                                try {
                                    emitter.send(SseEmitter.event().name("content")
                                            .data("{\"type\":\"content\",\"text\":\"" +
                                                    escapeJson(delta) + "\"}"));
                                } catch (Exception e) {
                                    log.error("Error sending content event: {}", e.getMessage());
                                }
                            }
                            break;

                        case "response.output_text.done":
                            log.info("Content generation completed");
                            break;

                        case "response.done":
                            log.info("Response completed");
                            break;

                        case "response.reasoning.message":
                            // Complete reasoning message (non-delta format)
                            if (jsonNode.has("content") || jsonNode.has("delta")) {
                                String content = jsonNode.has("content") ?
                                        jsonNode.get("content").asText() :
                                        jsonNode.get("delta").asText();
                                thinkingContent.append(content);
                                log.info("Reasoning message received, length: {}", content.length());

                                // Send thinking to client
                                try {
                                    emitter.send(SseEmitter.event().name("thinking")
                                            .data("{\"type\":\"thinking\",\"text\":\"" +
                                                    escapeJson(content) + "\"}"));
                                } catch (Exception e) {
                                    log.error("Error sending thinking event: {}", e.getMessage());
                                }
                            }
                            break;

                        default:
                            // Log unhandled event types for debugging
                            if (!eventType.isEmpty()) {
                                log.info("Unhandled event type: {}, full data: {}", eventType, data);
                            }
                            break;
                    }

                    // Check for end signal
                    if ("response.done".equals(eventType)) {
                        log.info("Received response.done signal, ending stream");
                        break;
                    }

                    // Also handle non-streaming format as fallback (complete output array)
                    if (jsonNode.has("output")) {
                        com.fasterxml.jackson.databind.JsonNode outputArray = jsonNode.get("output");
                        if (outputArray.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode outputItem : outputArray) {
                                String type = outputItem.has("type") ? outputItem.get("type").asText() : "";

                                if ("reasoning".equals(type) && outputItem.has("summary")) {
                                    com.fasterxml.jackson.databind.JsonNode summaryArray = outputItem.get("summary");
                                    if (summaryArray.isArray()) {
                                        for (com.fasterxml.jackson.databind.JsonNode summary : summaryArray) {
                                            if ("summary_text".equals(summary.get("type").asText()) && summary.has("text")) {
                                                String text = summary.get("text").asText();
                                                thinkingContent.append(text);
                                                log.info("Extracted reasoning from summary, length: {}", text.length());

                                                // Send thinking to client
                                                try {
                                                    emitter.send(SseEmitter.event().name("thinking")
                                                            .data("{\"type\":\"thinking\",\"text\":\"" +
                                                                    escapeJson(text) + "\"}"));
                                                } catch (Exception e) {
                                                    log.error("Error sending thinking event: {}", e.getMessage());
                                                }
                                            }
                                        }
                                    }
                                } else if ("message".equals(type) && outputItem.has("content")) {
                                    com.fasterxml.jackson.databind.JsonNode contentArray = outputItem.get("content");
                                    if (contentArray.isArray()) {
                                        for (com.fasterxml.jackson.databind.JsonNode contentItem : contentArray) {
                                            if ("output_text".equals(contentItem.get("type").asText()) && contentItem.has("text")) {
                                                String text = contentItem.get("text").asText();
                                                responseContent.append(text);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    log.warn("Error parsing streaming data: {}", e.getMessage());
                }
            }

            // Handle non-streaming response (complete JSON)
            if (!isStreaming && fullResponseBuilder.length() > 0) {
                log.info("Processing non-streaming response, length: {}", fullResponseBuilder.length());
                try {
                    String fullResponse = fullResponseBuilder.toString();
                    com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(fullResponse);

                    // Parse the complete response
                    if (jsonNode.has("output")) {
                        com.fasterxml.jackson.databind.JsonNode outputArray = jsonNode.get("output");
                        if (outputArray.isArray()) {
                            for (com.fasterxml.jackson.databind.JsonNode outputItem : outputArray) {
                                String type = outputItem.has("type") ? outputItem.get("type").asText() : "";

                                if ("reasoning".equals(type)) {
                                    if (outputItem.has("summary")) {
                                        com.fasterxml.jackson.databind.JsonNode summaryArray = outputItem.get("summary");
                                        if (summaryArray.isArray()) {
                                            for (com.fasterxml.jackson.databind.JsonNode summary : summaryArray) {
                                                if ("summary_text".equals(summary.get("type").asText()) && summary.has("text")) {
                                                    thinkingContent.append(summary.get("text").asText());
                                                }
                                            }
                                        }
                                    }
                                } else if ("message".equals(type)) {
                                    if (outputItem.has("content")) {
                                        com.fasterxml.jackson.databind.JsonNode contentArray = outputItem.get("content");
                                        if (contentArray.isArray()) {
                                            for (com.fasterxml.jackson.databind.JsonNode contentItem : contentArray) {
                                                if ("output_text".equals(contentItem.get("type").asText()) && contentItem.has("text")) {
                                                    String text = contentItem.get("text").asText();
                                                    responseContent.append(text);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Extract metadata
                    if (jsonNode.has("id")) {
                        responseId = jsonNode.get("id").asText();
                    }
                    if (jsonNode.has("model")) {
                        responseModel = jsonNode.get("model").asText();
                    }
                    if (jsonNode.has("usage")) {
                        com.fasterxml.jackson.databind.JsonNode usage = jsonNode.get("usage");
                        if (usage.has("total_tokens")) {
                            totalTokens = usage.get("total_tokens").asInt();
                        }
                    }

                    chunkCount = 1;
                    log.info("Non-streaming response parsed successfully");

                } catch (Exception e) {
                    log.error("Error parsing non-streaming response: {}", e.getMessage(), e);
                }
            }

            log.info("Finished reading stream. Total chunks received: {}, Response length: {}, Thinking length: {}",
                    chunkCount, responseContent.length(), thinkingContent.length());

            // Try to parse structured data from response
            String responseStr = responseContent.toString();
            String structuredDataJson = "null";
            if (responseStr != null && !responseStr.isBlank()) {
                try {
                    com.volcengine.imagegen.model.ChartInfo structuredData =
                            objectMapper.readValue(responseStr, com.volcengine.imagegen.model.ChartInfo.class);
                    if (structuredData != null && structuredData.charts() != null && !structuredData.charts().isEmpty()) {
                        structuredDataJson = objectMapper.writeValueAsString(structuredData);
                        log.info("Successfully parsed {} charts from response", structuredData.charts().size());
                    }
                } catch (Exception e) {
                    log.debug("Response is not valid JSON chart format: {}", e.getMessage());
                }
            }

            // Build thinking content - if empty, try to get from response
            String thinkingStr = thinkingContent.toString();
            if (thinkingStr.isEmpty() && responseStr != null && !responseStr.isEmpty()) {
                // Try to extract thinking from the response if it's in a different format
                log.info("Thinking content empty, trying to extract from response");
                // The thinking might be embedded in the response or in a format we need to parse differently
                thinkingStr = "";  // Keep empty for now
            }

            log.info("Final result - Response: {} chars, Thinking: {} chars, Structured: {}",
                    responseStr != null ? responseStr.length() : 0,
                    thinkingStr.length(),
                    structuredDataJson.length());

            // Send final result
            String resultJson = String.format(
                    "{" +
                            "\"model\":\"%s\"," +
                            "\"thinking\":\"%s\"," +
                            "\"response\":\"%s\"," +
                            "\"structured_data\":%s," +
                            "\"request_id\":\"%s\"," +
                            "\"tokens_used\":%d" +
                            "}",
                    responseModel != null ? responseModel : "",
                    escapeJson(thinkingStr),
                    escapeJson(responseStr),
                    structuredDataJson,
                    responseId != null ? responseId : "",
                    totalTokens
            );

            try {
                emitter.send(SseEmitter.event().name("result").data(resultJson));
                emitter.complete();
                log.info("SSE emitter completed successfully. Final result sent with thinking: {} chars, response: {} chars",
                        thinkingStr.length(), responseStr != null ? responseStr.length() : 0);
            } catch (Exception e) {
                log.error("Error sending final result: {}", e.getMessage());
                emitter.completeWithError(e);
            }

            log.info("Streaming analysis completed successfully. Chunks: {}, Response length: {}",
                    chunkCount, responseStr != null ? responseStr.length() : 0);

        } catch (Exception e) {
            log.error("Error in streaming analysis", e);
            throw e;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
