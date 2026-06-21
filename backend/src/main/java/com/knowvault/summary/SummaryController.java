package com.knowvault.summary;

import com.knowvault.summary.dto.SummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content/{contentId}/summaries")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @PostMapping
    public ResponseEntity<SummaryResponse> generate(
            @PathVariable Long contentId,
            @RequestBody Map<String, String> body) {
        String type = body.getOrDefault("type", "medium");
        SummaryResponse response = summaryService.generateSummary(contentId, type);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SummaryResponse>> listAll(@PathVariable Long contentId) {
        return ResponseEntity.ok(summaryService.getSummaries(contentId));
    }

    @GetMapping("/{type}")
    public ResponseEntity<SummaryResponse> getByType(
            @PathVariable Long contentId,
            @PathVariable String type) {
        SummaryResponse response = summaryService.getSummary(contentId, type);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
