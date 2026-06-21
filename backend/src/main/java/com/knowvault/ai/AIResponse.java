package com.knowvault.ai;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AIResponse {
    private String title;
    private String summary;
    private List<String> keyPoints;
    private List<String> suggestedTags;
}
