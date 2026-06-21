package com.knowvault.import_.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportStatus {
    private String importId;
    private String status;
    private int progress;
    private String message;
}
