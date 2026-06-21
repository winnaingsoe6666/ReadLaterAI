package com.knowvault.content;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentRepository contentRepository;

    public ContentController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Content>> listAll() {
        return ResponseEntity.ok(contentRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Content> getById(@PathVariable Long id) {
        return contentRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Content>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(contentRepository.searchByFullText(query));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Content> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return contentRepository.findById(id).map(content -> {
            content.setStatus(body.getOrDefault("status", content.getStatus()));
            return ResponseEntity.ok(contentRepository.save(content));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<Content> toggleFavorite(@PathVariable Long id) {
        return contentRepository.findById(id).map(content -> {
            content.setFavorite(!Boolean.TRUE.equals(content.getFavorite()));
            return ResponseEntity.ok(contentRepository.save(content));
        }).orElse(ResponseEntity.notFound().build());
    }
}
