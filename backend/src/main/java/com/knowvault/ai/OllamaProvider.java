package com.knowvault.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "knowvault.ai.provider", havingValue = "ollama")
public class OllamaProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OllamaProvider.class);

    private final RestTemplate restTemplate;
    private final String model;
    private final String baseUrl;
    private final RetryConfig retryConfig;
    private final ContentChunker chunker;
    private final ObjectMapper objectMapper;

    public OllamaProvider(
            @Value("${knowvault.ai.ollama.base-url}") String baseUrl,
            @Value("${knowvault.ai.ollama.model}") String model) {
        this.model = model;
        this.baseUrl = baseUrl;
        this.retryConfig = RetryConfig.defaults();
        this.chunker = new ContentChunker();
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000); // Increased for longer content
        this.restTemplate = new RestTemplate(factory);
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

        return callOllamaWithRetry(content, length);
    }

    /**
     * Summarize long content by chunking, then combining results.
     */
    private AIResponse summarizeChunked(String content, SummaryLength length) {
        List<String> chunks = chunker.chunk(content);
        log.info("Summarizing {} chunks with Ollama", chunks.size());

        List<String> chunkSummaries = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            log.info("Processing chunk {}/{}", i + 1, chunks.size());
            AIResponse chunkResponse = callOllamaWithRetry(chunks.get(i), SummaryLength.SHORT);
            chunkSummaries.add(chunkResponse.getSummary());
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

        return callOllamaWithRetry(combinePrompt, length);
    }

    /**
     * Call Ollama API with retry logic.
     */
    private AIResponse callOllamaWithRetry(String content, SummaryLength length) {
        return retryConfig.execute(() -> {
            String lengthInstruction = switch (length) {
                case SHORT -> "2-3 sentences";
                case MEDIUM -> "one detailed paragraph";
                case DETAILED -> "multiple paragraphs with bullet-point key takeaways";
            };

            String prompt = "Summarize the following content in " + lengthInstruction + ". " +
                    "Return valid JSON with keys: title (string), summary (string), key_points (array of strings), tags (array of strings). " +
                    "Content:\n\n" + content;

            Map<String, Object> request = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/api/generate", request, Map.class);

            return parseResponse(response);
        }, "Ollama");
    }

    /**
     * Parse Ollama API response into AIResponse.
     */
    @SuppressWarnings("unchecked")
    private AIResponse parseResponse(Map<String, Object> response) {
        try {
            String text = (String) response.get("response");
            if (text == null || text.isBlank()) {
                throw new RuntimeException("Empty response from Ollama");
            }

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
            throw new RuntimeException("Failed to parse Ollama response: " + e.getMessage(), e);
        }
    }
}
