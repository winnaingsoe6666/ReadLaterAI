package com.knowvault.summary.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SummaryResponse {
    private Long id;
    private Long contentId;
    private String summaryType;
    private String summary;
    private List<String> keyPoints;
    private String generatedAt;
}
