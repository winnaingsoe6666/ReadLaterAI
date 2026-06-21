package com.knowvault.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "knowvault.ai.provider", havingValue = "gemini")
public class GeminiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private final WebClient webClient;
    private final String model;
    private final String apiKey;
    private final RetryConfig retryConfig;
    private final ContentChunker chunker;
    private final ObjectMapper objectMapper;

    public GeminiProvider(
            @Value("${knowvault.ai.gemini.api-key}") String apiKey,
            @Value("${knowvault.ai.gemini.model}") String model) {
        this.model = model;
        this.apiKey = apiKey;
        this.retryConfig = RetryConfig.defaults();
        this.chunker = new ContentChunker();
        this.objectMapper = new ObjectMapper();

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30)); // Increased from 10s
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public AIResponse summarize(String content, SummaryLength length) {
        if (content == null || content.isBlank()) {
            return AIResponse.builder()
                    .title("Empty Content")
                    .summary("No content to summarize.")
                    .keyPoints(List.of())
                    .suggestedTags(List.of())
                    .build();
        }

        // Chunk if content is too long
        if (chunker.needsChunking(content)) {
            return summarizeChunked(content, length);
        }

        return callGeminiWithRetry(content, length);
    }

    /**
     * Summarize long content by chunking, then combining results.
     */
    private AIResponse summarizeChunked(String content, SummaryLength length) {
        List<String> chunks = chunker.chunk(content);
        log.info("Summarizing {} chunks with Gemini", chunks.size());

        List<String> chunkSummaries = new ArrayList<>();
        List<String> allKeyPoints = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            log.info("Processing chunk {}/{}", i + 1, chunks.size());
            AIResponse chunkResponse = callGeminiWithRetry(chunks.get(i), SummaryLength.SHORT);
            chunkSummaries.add(chunkResponse.getSummary());
            if (chunkResponse.getKeyPoints() != null) {
                allKeyPoints.addAll(chunkResponse.getKeyPoints());
            }
        }

        // Combine chunk summaries into final summary
        String combinedText = String.join("\n\n", chunkSummaries);
        String lengthInstruction = switch (length) {
            case SHORT -> "2-3 sentences";
            case MEDIUM -> "one detailed paragraph";
            case DETAILED -> "multiple paragraphs with bullet-point key takeaways";
        };

        String combinePrompt = "Combine these summaries into one cohesive " + lengthInstruction + " summary. " +
                "Return valid JSON with keys: title (string), summary (string), key_points (array of strings), tags (array of strings).\n\n" +
                "Summaries to combine:\n" + combinedText;

        return callGeminiWithRetry(combinePrompt, length);
    }

    /**
     * Call Gemini API with retry logic.
     */
    private AIResponse callGeminiWithRetry(String content, SummaryLength length) {
        return retryConfig.execute(() -> {
            String lengthInstruction = switch (length) {
                case SHORT -> "2-3 sentences";
                case MEDIUM -> "one detailed paragraph";
                case DETAILED -> "multiple paragraphs with bullet-point key takeaways";
            };

            String prompt = "Summarize the following content in " + lengthInstruction + ". " +
                    "Return valid JSON with keys: title (string), summary (string), key_points (array of strings), tags (array of strings). " +
                    "Content:\n\n" + content;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri("/{model}:generateContent?key={key}", model, apiKey)
                    .bodyValue(Map.of("contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt))))))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return parseResponse(response);
        }, "Gemini");
    }

    /**
     * Parse Gemini API response into AIResponse.
     */
    @SuppressWarnings("unchecked")
    private AIResponse parseResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new RuntimeException("No candidates in Gemini response");
            }

            Map<String, Object> content0 = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content0.get("parts");
            String text = (String) parts.get(0).get("text");

            // Parse JSON from response (strip markdown fences)
            String json = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            return AIResponse.builder()
                    .title((String) parsed.get("title"))
                    .summary((String) parsed.get("summary"))
                    .keyPoints(parsed.containsKey("key_points") ? (List<String>) parsed.get("key_points") : List.of())
                    .suggestedTags(parsed.containsKey("tags") ? (List<String>) parsed.get("tags") : List.of())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }
}
