package com.knowvault.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
     * Update AI settings in application.yml using proper YAML parsing.
     * Note: Requires app restart to take effect.
     */
    @SuppressWarnings("unchecked")
    public AISettingsDTO updateSettings(AISettingsDTO settings) {
        try {
            Path configPath = findApplicationYml();
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(Files.readString(configPath));

            // Navigate to knowvault → ai section
            Map<String, Object> knowvault = (Map<String, Object>) root.get("knowvault");
            if (knowvault == null) {
                throw new RuntimeException("'knowvault' section not found in application.yml");
            }

            Map<String, Object> ai = (Map<String, Object>) knowvault.get("ai");
            if (ai == null) {
                ai = new java.util.LinkedHashMap<>();
                knowvault.put("ai", ai);
            }

            // Update values with proper YAML structure
            ai.put("provider", settings.getProvider());

            // Gemini sub-section
            Map<String, Object> gemini = (Map<String, Object>) ai.get("gemini");
            if (gemini == null) {
                gemini = new java.util.LinkedHashMap<>();
                ai.put("gemini", gemini);
            }
            gemini.put("api-key", settings.getGeminiApiKey());
            gemini.put("model", settings.getGeminiModel());

            // Ollama sub-section
            Map<String, Object> ollama = (Map<String, Object>) ai.get("ollama");
            if (ollama == null) {
                ollama = new java.util.LinkedHashMap<>();
                ai.put("ollama", ollama);
            }
            ollama.put("base-url", settings.getOllamaBaseUrl());
            ollama.put("model", settings.getOllamaModel());

            // Write back with comments preserved via manual formatting
            // SnakeYAML dump doesn't preserve comments, so we format nicely
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml dumper = new Yaml(options);
            Files.writeString(configPath, dumper.dump(root));

            log.info("AI settings written to {}. Restart required for changes to take effect.", configPath);

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
     * Mask API key for display (show only first 8 and last 4 chars).
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.length() < 12) {
            return apiKey;
        }
        return apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
