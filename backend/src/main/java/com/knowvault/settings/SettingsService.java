package com.knowvault.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    @Value("${knowvault.ai.provider:none}")
    private String provider;

    @Value("${knowvault.ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${knowvault.ai.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${knowvault.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${knowvault.ai.ollama.model:llama3}")
    private String ollamaModel;

    /**
     * Get current AI settings.
     */
    public AISettingsDTO getSettings() {
        return AISettingsDTO.builder()
                .provider(provider)
                .geminiApiKey(maskApiKey(geminiApiKey))
                .geminiModel(geminiModel)
                .ollamaBaseUrl(ollamaBaseUrl)
                .ollamaModel(ollamaModel)
                .build();
    }

    /**
     * Update AI settings in application.yml.
     * Note: Requires app restart to take effect.
     */
    public AISettingsDTO updateSettings(AISettingsDTO settings) {
        try {
            Path configPath = findApplicationYml();
            String content = Files.readString(configPath);

            content = updateYamlValue(content, "knowvault.ai.provider", settings.getProvider());
            content = updateYamlValue(content, "knowvault.ai.gemini.api-key", settings.getGeminiApiKey());
            content = updateYamlValue(content, "knowvault.ai.gemini.model", settings.getGeminiModel());
            content = updateYamlValue(content, "knowvault.ai.ollama.base-url", settings.getOllamaBaseUrl());
            content = updateYamlValue(content, "knowvault.ai.ollama.model", settings.getOllamaModel());

            Files.writeString(configPath, content);
            log.info("AI settings updated. Restart required for changes to take effect.");

            // Update in-memory values
            this.provider = settings.getProvider();
            this.geminiApiKey = settings.getGeminiApiKey();
            this.geminiModel = settings.getGeminiModel();
            this.ollamaBaseUrl = settings.getOllamaBaseUrl();
            this.ollamaModel = settings.getOllamaModel();

            return getSettings();
        } catch (IOException e) {
            throw new RuntimeException("Failed to update settings: " + e.getMessage(), e);
        }
    }

    /**
     * Find application.yml location.
     */
    private Path findApplicationYml() {
        // Try multiple locations
        List<String> paths = List.of(
                "src/main/resources/application.yml",
                "backend/src/main/resources/application.yml",
                "../backend/src/main/resources/application.yml"
        );

        for (String path : paths) {
            Path p = Path.of(path);
            if (Files.exists(p)) {
                return p.toAbsolutePath();
            }
        }

        throw new RuntimeException("Could not find application.yml");
    }

    /**
     * Update a YAML value (simple key-value replacement).
     */
    private String updateYamlValue(String content, String key, String value) {
        String prefix = key + ":";
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        boolean found = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix) && !found) {
                // Preserve indentation
                String indent = line.substring(0, line.indexOf(trimmed));
                result.append(indent).append(key).append(": ").append(value);
                found = true;
            } else {
                result.append(line);
            }
            result.append("\n");
        }

        if (!found) {
            log.warn("Key '{}' not found in YAML, skipping update", key);
        }

        return result.toString();
    }

    /**
     * Mask API key for display (show only first 8 and last 4 chars).
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.length() < 12) {
            return apiKey;
        }
        return apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
