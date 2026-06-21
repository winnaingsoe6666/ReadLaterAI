package com.knowvault.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "knowvault.ai.provider", havingValue = "ollama")
public class OllamaProvider implements AIProvider {

    private final RestTemplate restTemplate;
    private final String model;
    private final String baseUrl;

    public OllamaProvider(
            @Value("${knowvault.ai.ollama.base-url}") String baseUrl,
            @Value("${knowvault.ai.ollama.model}") String model) {
        this.model = model;
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
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

        Map<String, Object> request = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(baseUrl + "/api/generate", request, Map.class);
        String text = (String) response.get("response");

        String json = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        ObjectMapper mapper = new ObjectMapper();
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
