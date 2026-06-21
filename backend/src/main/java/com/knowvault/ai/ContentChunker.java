package com.knowvault.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits long content into manageable chunks for AI processing.
 * Uses sentence boundaries to avoid splitting mid-sentence.
 */
public class ContentChunker {

    private static final Logger log = LoggerFactory.getLogger(ContentChunker.class);

    /** Default max characters per chunk (~3000 tokens, conservative estimate) */
    private static final int DEFAULT_CHUNK_SIZE = 12000;

    private final int chunkSize;

    public ContentChunker(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public ContentChunker() {
        this(DEFAULT_CHUNK_SIZE);
    }

    /**
     * Split content into chunks. Returns single-element list if content fits in one chunk.
     */
    public List<String> chunk(String content) {
        if (content == null || content.isEmpty()) {
            return List.of("");
        }

        if (content.length() <= chunkSize) {
            return List.of(content);
        }

        log.info("Content length {} exceeds chunk size {}, splitting...",
                content.length(), chunkSize);

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            // Try to break at sentence boundary
            if (end < content.length()) {
                int lastPeriod = content.lastIndexOf('.', end);
                int lastNewline = content.lastIndexOf('\n', end);
                int breakPoint = Math.max(lastPeriod, lastNewline);

                if (breakPoint > start + chunkSize / 2) {
                    end = breakPoint + 1; // Include the punctuation
                }
            }

            chunks.add(content.substring(start, end).trim());
            start = end;
        }

        log.info("Split into {} chunks", chunks.size());
        return chunks;
    }

    /**
     * Check if content needs chunking.
     */
    public boolean needsChunking(String content) {
        return content != null && content.length() > chunkSize;
    }
}
