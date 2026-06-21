package com.knowvault.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "knowvault.ai.provider", havingValue = "gemini")
public class GeminiProvider implements AIProvider {

    private final WebClient webClient;
    private final String model;
    private final String apiKey;

    public GeminiProvider(
            @Value("${knowvault.ai.gemini.api-key}") String apiKey,
            @Value("${knowvault.ai.gemini.model}") String model) {
        this.model = model;
        this.apiKey = apiKey;
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10));
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public AIResponse summarize(String content, SummaryLength length) {
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

        // Parse response — extract text from candidates[0].content.parts[0].text
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        @SuppressWarnings("unchecked")
        Map<String, Object> content0 = (Map<String, Object>) candidates.get(0).get("content");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content0.get("parts");
        String text = (String) parts.get(0).get("text");

        // Parse the JSON from the text (strip markdown code fences if present)
        String json = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            Map<String, Object> parsed = mapper.readValue(json, Map.class);
            return AIResponse.builder()
                    .title((String) parsed.get("title"))
                    .summary((String) parsed.get("summary"))
                    .keyPoints(parsed.containsKey("key_points") ? (List<String>) parsed.get("key_points") : List.of())
                    .suggestedTags(parsed.containsKey("tags") ? (List<String>) parsed.get("tags") : List.of())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
}
