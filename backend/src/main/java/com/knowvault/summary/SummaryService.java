package com.knowvault.summary;

import com.knowvault.ai.AIProvider;
import com.knowvault.ai.AIResponse;
import com.knowvault.ai.SummaryLength;
import com.knowvault.content.Content;
import com.knowvault.content.ContentRepository;
import com.knowvault.summary.dto.SummaryResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final ContentRepository contentRepository;
    private final AIProvider aiProvider;
    private final ObjectMapper objectMapper;

    public SummaryService(SummaryRepository summaryRepository,
                          ContentRepository contentRepository,
                          AIProvider aiProvider) {
        this.summaryRepository = summaryRepository;
        this.contentRepository = contentRepository;
        this.aiProvider = aiProvider;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public SummaryResponse generateSummary(Long contentId, String summaryType) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found: " + contentId));

        SummaryLength length = switch (summaryType) {
            case "short" -> SummaryLength.SHORT;
            case "detailed" -> SummaryLength.DETAILED;
            default -> SummaryLength.MEDIUM;
        };

        AIResponse aiResponse = aiProvider.summarize(content.getContentText(), length);

        // Serialize keyPoints to JSON string for storage
        String keyPointsJson;
        try {
            keyPointsJson = objectMapper.writeValueAsString(
                    aiResponse.getKeyPoints() != null ? aiResponse.getKeyPoints() : List.of());
        } catch (Exception e) {
            keyPointsJson = "[]";
        }

        // Upsert: find existing or create new
        Summary summary = summaryRepository.findByContentIdAndSummaryType(contentId, summaryType)
                .orElse(new Summary());
        summary.setContent(content);
        summary.setSummaryType(summaryType);
        summary.setSummary(aiResponse.getSummary());
        summary.setKeyPoints(keyPointsJson);
        summary.setGeneratedAt(java.time.Instant.now().toString());

        Summary saved = summaryRepository.save(summary);
        return toResponse(saved);
    }

    public SummaryResponse getSummary(Long contentId, String summaryType) {
        return summaryRepository.findByContentIdAndSummaryType(contentId, summaryType)
                .map(this::toResponse)
                .orElse(null);
    }

    public List<SummaryResponse> getSummaries(Long contentId) {
        return summaryRepository.findByContentId(contentId).stream()
                .map(this::toResponse)
                .toList();
    }

    private SummaryResponse toResponse(Summary summary) {
        List<String> keyPoints;
        try {
            keyPoints = objectMapper.readValue(
                    summary.getKeyPoints() != null ? summary.getKeyPoints() : "[]",
                    new TypeReference<>() {});
        } catch (Exception e) {
            keyPoints = List.of();
        }

        return SummaryResponse.builder()
                .id(summary.getId())
                .contentId(summary.getContent().getId())
                .summaryType(summary.getSummaryType())
                .summary(summary.getSummary())
                .keyPoints(keyPoints)
                .generatedAt(summary.getGeneratedAt())
                .build();
    }
}
