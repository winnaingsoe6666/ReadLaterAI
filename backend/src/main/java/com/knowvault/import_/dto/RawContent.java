package com.knowvault.import_.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RawContent {
    private String title;
    private String contentText;
    private String url;
    private String timestamp;
    private String author;
    private String sourceType;
}
