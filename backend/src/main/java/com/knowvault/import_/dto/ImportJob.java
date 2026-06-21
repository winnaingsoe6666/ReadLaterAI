package com.knowvault.import_.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ImportJob {
    private String id;
    private String status;
    private int totalItems;
    private int processedItems;
    private int importedItems;
    private int skippedItems;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
