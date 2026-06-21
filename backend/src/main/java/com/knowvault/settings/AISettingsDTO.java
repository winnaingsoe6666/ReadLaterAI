package com.knowvault.settings;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AISettingsDTO {
    private String provider;          // none, gemini, ollama
    private String geminiApiKey;
    private String geminiModel;
    private String ollamaBaseUrl;
    private String ollamaModel;
}
