package com.studyassistant.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OpenAIService - Handles all communication with OpenAI API
 *
 * - Generates embeddings (single or batched) with retry + backoff
 * - Generates chat completions (with light retry)
 */
@Service
public class OpenAIService {

    // ====== Config (injected from application.properties) ======
    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl; // e.g., https://api.openai.com/v1

    @Value("${openai.embedding.model}")
    private String embeddingModel; // e.g., text-embedding-3-small

    @Value("${openai.chat.model}")
    private String chatModel;

    // Minimum gap between ANY two embedding HTTP calls (global pacing)
    // You can tune this at runtime via application.properties
    @Value("${openai.min-interval-ms:800}")
    private long minIntervalMs;

    // ====== HTTP + JSON ======
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // ====== Retry/backoff controls ======
    private static final int MAX_RETRIES = 10;
    private static final long BASE_BACKOFF_MS = 3000L;
    private static final long MAX_BACKOFF_MS = 30000L;

    // ====== Global simple rate limiter (no extra deps) ======
    private final Object rateLock = new Object();
    private long lastCallAtMs = 0L;

    public OpenAIService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ------------------------------------------------------------
    // Embeddings (BATCH)
    // ------------------------------------------------------------
    /**
     * Batch embeddings request. Sends input as an array of strings to cut request count.
     * Includes global pacing + exponential backoff and Retry-After handling for 429/5xx.
     */
    public List<float[]> generateEmbeddingsBatch(List<String> texts) throws IOException {
        if (texts == null || texts.isEmpty())
            return Collections.emptyList();
        
        System.out.println("🔑 Loaded API key prefix: " +
        (apiKey == null ? "null" : apiKey.substring(0, Math.min(10, apiKey.length())) + "..."));

        Request request = buildEmbeddingsRequest(texts);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            // global pace so multiple uploads don't hammer the API
            respectGlobalRateLimit();

            //  START: add timing + debug logs
            long t0 = System.currentTimeMillis();
            System.out.println("[embed] attempt " + attempt +
                    " | batchSize=" + texts.size() +
                    " | at " + t0);
            //  END: debug log

            try (Response response = httpClient.newCall(request).execute()) {

                //  record completion time
                long t1 = System.currentTimeMillis();
                System.out.println("[embed] HTTP " + response.code() +
                        " in " + (t1 - t0) + " ms");
                //  END timing log

                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body == null)
                        throw new IOException("OpenAI empty response body");

                    String responseBody = body.string();
                    JsonNode json = objectMapper.readTree(responseBody);
                    JsonNode data = json.get("data");

                    if (data == null || !data.isArray()) {
                        throw new IOException("OpenAI embeddings response missing 'data' array");
                    }

                    List<float[]> out = new ArrayList<>(data.size());
                    for (int i = 0; i < data.size(); i++) {
                        JsonNode embNode = data.get(i).get("embedding");
                        if (embNode == null || !embNode.isArray()) {
                            throw new IOException("OpenAI embeddings response missing 'embedding' at index " + i);
                        }
                        float[] vec = new float[embNode.size()];
                        for (int j = 0; j < embNode.size(); j++) {
                            vec[j] = (float) embNode.get(j).asDouble();
                        }
                        out.add(vec);
                    }
                    return out; // success
                }

                int code = response.code();
                if (code == 429 || code >= 500) {
                    long raSec = retryAfterSeconds(response);
                    long sleepMs = (raSec > 0) ? raSec * 1000L : computeBackoffMs(BASE_BACKOFF_MS, attempt);
                    System.out.println("[embed] will retry after " + sleepMs + " ms"); //  extra log
                    quietSleep(sleepMs);
                    continue; // retry
                }

                // Non-retryable (400/401/403/404 etc.)
                throw new IOException("OpenAI embeddings error: " + code + " - " + response.message());

            } catch (IOException io) {
                if (attempt == MAX_RETRIES)
                    throw io;
                long backoff = computeBackoffMs(BASE_BACKOFF_MS, attempt);
                System.out.println("[embed] IOException, retrying after " + backoff + " ms: " + io.getMessage()); // 
                quietSleep(backoff);
            }
        }
        throw new IOException("OpenAI embeddings failed after retries");
    }
    

    // ------------------------------------------------------------
    // Embeddings (single) – thin wrapper over batch
    // ------------------------------------------------------------
    public float[] generateEmbedding(String text) throws IOException {
        List<float[]> list = generateEmbeddingsBatch(List.of(text));
        return list.isEmpty() ? new float[0] : list.get(0);
    }

    // ------------------------------------------------------------
    // Chat Completion (with light retry/backoff)
    // ------------------------------------------------------------
    public String generateChatCompletion(String systemPrompt, String userMessage) throws IOException {
        Request request = buildChatRequest(systemPrompt, userMessage);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            // chat usually has lower rate; still pace a bit
            respectGlobalRateLimit();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    ResponseBody body = response.body();
                    if (body == null) throw new IOException("OpenAI empty response body");
                    String responseBody = body.string();
                    JsonNode json = objectMapper.readTree(responseBody);
                    JsonNode choices = json.get("choices");
                    if (choices == null || !choices.isArray() || choices.isEmpty()) {
                        throw new IOException("OpenAI chat response missing choices");
                    }
                    JsonNode content = choices.get(0).get("message").get("content");
                    return content == null ? "" : content.asText();
                }

                int code = response.code();
                if (code == 429 || code >= 500) {
                    long raSec = retryAfterSeconds(response);
                    if (raSec > 0) {
                        quietSleep(raSec * 1000L);
                    } else {
                        quietSleep(computeBackoffMs(BASE_BACKOFF_MS, attempt));
                    }
                    continue;
                }

                throw new IOException("OpenAI chat error: " + code + " - " + response.message());

            } catch (IOException io) {
                if (attempt == MAX_RETRIES) throw io;
                quietSleep(computeBackoffMs(BASE_BACKOFF_MS, attempt));
            }
        }
        throw new IOException("OpenAI chat failed after retries");
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
    private Request buildEmbeddingsRequest(Object input) throws IOException {
        // input is either String or List<String>
        var root = objectMapper.createObjectNode();
        root.put("model", embeddingModel);
        if (input instanceof String s) {
            root.put("input", s);
        } else {
            root.set("input", objectMapper.valueToTree(input));
        }
        String jsonBody = objectMapper.writeValueAsString(root);
        System.out.println("Auth header = Bearer " +
        (apiKey == null ? "null" : apiKey.substring(0, 10) + "..."));

        return new Request.Builder()
                .url(apiUrl + "/embeddings")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
    }

    private Request buildChatRequest(String systemPrompt, String userMessage) throws IOException {
        var root = objectMapper.createObjectNode();
        root.put("model", chatModel);
        var messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode()
                .put("role", "system")
                .put("content", systemPrompt == null ? "" : systemPrompt));
        messages.add(objectMapper.createObjectNode()
                .put("role", "user")
                .put("content", userMessage == null ? "" : userMessage));
        root.set("messages", messages);
        root.put("temperature", 0.7);

        String jsonBody = objectMapper.writeValueAsString(root);

        return new Request.Builder()
                .url(apiUrl + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
    }

    private void respectGlobalRateLimit() {
        synchronized (rateLock) {
            long now = System.currentTimeMillis();
            long wait = lastCallAtMs + minIntervalMs - now;
            if (wait > 0) {
                try { Thread.sleep(wait); } catch (InterruptedException ignored) {}
                now = System.currentTimeMillis();
            }
            lastCallAtMs = now;
        }
    }

    private static long computeBackoffMs(long baseMs, int attempt) {
        long backoff = (long) (baseMs * Math.pow(2, attempt - 1));
        if (backoff > MAX_BACKOFF_MS) backoff = MAX_BACKOFF_MS;
        long jitter = (long) (backoff * 0.25); // +/- 25%
        long low = backoff - jitter, high = backoff + jitter;
        return low + (long) (Math.random() * (high - low + 1));
    }

    private static long retryAfterSeconds(Response response) {
        String ra = response.header("Retry-After");
        if (ra == null) return -1;
        try { return Long.parseLong(ra); } catch (NumberFormatException ignore) { return -1; }
    }

    private static void quietSleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // Convert float array to a CSV string (unchanged)
    public String embeddingToString(float[] embedding) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        return sb.toString();
    }
}
