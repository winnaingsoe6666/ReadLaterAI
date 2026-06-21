package com.knowvault.settings;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/ai")
    public ResponseEntity<AISettingsDTO> getAISettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping("/ai")
    public ResponseEntity<AISettingsDTO> updateAISettings(@RequestBody AISettingsDTO settings) {
        return ResponseEntity.ok(settingsService.updateSettings(settings));
    }
}
