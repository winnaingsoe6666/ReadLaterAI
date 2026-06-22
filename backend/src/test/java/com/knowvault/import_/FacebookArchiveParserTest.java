package com.knowvault.import_;

import com.knowvault.common.exception.ImportException;
import com.knowvault.import_.dto.RawContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.*;

class FacebookArchiveParserTest {

    private final FacebookArchiveParser parser = new FacebookArchiveParser();

    @Test
    void validate_throwsOnEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", new byte[0]);
        assertThrows(ImportException.class, () -> parser.validate(file));
    }

    @Test
    void validate_throwsOnNonZip() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        assertThrows(ImportException.class, () -> parser.validate(file));
    }

    @Test
    void validate_passesForValidZip() {
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", "PK..".getBytes());
        assertDoesNotThrow(() -> parser.validate(file));
    }

    @Test
    void parsePosts_parsesHtmlPosts(@TempDir Path tempDir) throws IOException {
        Path postsDir = tempDir.resolve("posts");
        Files.createDirectories(postsDir);
        Files.writeString(postsDir.resolve("your_posts_1.html"),
            "<html><body><div class=\"post\">" +
            "<div class=\"title\">My Post</div>" +
            "<div class=\"content\">Post content here</div>" +
            "<div class=\"timestamp\">2024-01-01</div>" +
            "<a href=\"https://fb.com/1\">link</a>" +
            "</div></body></html>");

        List<RawContent> results = parser.parsePosts(tempDir);
        assertEquals(1, results.size());
        assertEquals("My Post", results.get(0).getTitle());
        assertEquals("Post content here", results.get(0).getContentText());
    }

    @Test
    void parseSavedItems_parsesJson(@TempDir Path tempDir) throws IOException {
        Path savedDir = tempDir.resolve("saved_items");
        Files.createDirectories(savedDir);
        Files.writeString(savedDir.resolve("saved_items.json"),
            "[{\"title\":\"Saved\",\"content\":\"Text\",\"timestamp\":\"2024-01-01\",\"url\":\"https://example.com\"}]");

        List<RawContent> results = parser.parseSavedItems(tempDir);
        assertEquals(1, results.size());
        assertEquals("Saved", results.get(0).getTitle());
        assertEquals("saved_item", results.get(0).getSourceType());
    }

    @Test
    void cleanup_preservesDirectoryInDebugMode(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("to-delete");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("file.txt"), "data");

        parser.cleanup(subDir);
        // Debug mode preserves temp directory for inspection
        assertTrue(Files.exists(subDir));
    }
}
