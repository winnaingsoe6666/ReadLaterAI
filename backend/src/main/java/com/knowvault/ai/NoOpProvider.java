package com.knowvault.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConditionalOnProperty(name = "knowvault.ai.provider", havingValue = "none", matchIfMissing = true)
public class NoOpProvider implements AIProvider {

    @Override
    public AIResponse summarize(String content, SummaryLength length) {
        return AIResponse.builder()
                .title("AI Not Configured")
                .summary("AI not configured. Set knowvault.ai.provider to gemini or ollama.")
                .keyPoints(List.of())
                .suggestedTags(List.of())
                .build();
    }
}
