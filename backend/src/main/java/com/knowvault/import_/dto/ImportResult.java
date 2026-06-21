package com.knowvault.import_.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportResult {
    private int imported;
    private int skipped;
    private int total;
}
