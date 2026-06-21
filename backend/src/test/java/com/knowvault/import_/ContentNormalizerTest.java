package com.knowvault.import_;

import com.knowvault.content.Content;
import com.knowvault.import_.dto.RawContent;
import com.knowvault.tag.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ContentNormalizerTest {

    private final ContentNormalizer normalizer = new ContentNormalizer();

    @Test
    void normalize_mapsFieldsCorrectly() {
        RawContent raw = RawContent.builder()
            .title("Test Title")
            .contentText("Some content")
            .url("https://example.com")
            .timestamp("2024-01-01")
            .author("John")
            .sourceType("post")
            .build();

        Content result = normalizer.normalize(raw);
        assertEquals("Test Title", result.getTitle());
        assertEquals("Some content", result.getContentText());
        assertEquals("https://example.com", result.getUrl());
        assertEquals("facebook", result.getSource());
        assertEquals("John", result.getAuthor());
        assertEquals("2024-01-01", result.getCreatedDate());
        assertEquals("unread", result.getStatus());
        assertFalse(result.getFavorite());
    }

    @Test
    void normalize_handlesNullFields() {
        RawContent raw = RawContent.builder().build();
        Content result = normalizer.normalize(raw);
        assertEquals("", result.getTitle());
        assertEquals("", result.getContentText());
        assertEquals("", result.getUrl());
    }

    @Test
    void extractTags_returnsTagsFromContent() {
        RawContent raw = RawContent.builder()
            .title("Java Programming Tutorial")
            .contentText("Learn Java programming with this comprehensive guide")
            .build();

        Set<Tag> tags = normalizer.extractTags(raw);
        assertFalse(tags.isEmpty());
        assertTrue(tags.stream().anyMatch(t -> t.getName().equals("post") || t.getName().length() > 0));
    }

    @Test
    void categorize_detectsTechnologyCategory() {
        RawContent raw = RawContent.builder()
            .title("Java Programming Guide")
            .contentText("Learn software development with Java and Python")
            .build();

        String category = normalizer.categorize(raw);
        assertEquals("technology", category);
    }

    @Test
    void categorize_defaultsToGeneral() {
        RawContent raw = RawContent.builder()
            .title("Random stuff")
            .contentText("Nothing specific here")
            .build();

        String category = normalizer.categorize(raw);
        assertEquals("general", category);
    }
}
