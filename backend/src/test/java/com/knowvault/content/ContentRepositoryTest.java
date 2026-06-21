package com.knowvault.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ContentRepositoryTest {

    @Autowired
    private ContentRepository contentRepository;

    private Content createContent(String title, String url, String source, String status) {
        Content content = new Content();
        content.setTitle(title);
        content.setUrl(url);
        content.setSource(source);
        content.setStatus(status);
        content.setFavorite(false);
        content.setImportDate("2024-01-01T00:00:00");
        return content;
    }

    @Test
    void saveAndFindByUrlAndSource() {
        Content content = createContent("Test", "https://example.com/1", "facebook", "unread");
        content.setContentText("Body");
        contentRepository.save(content);

        Optional<Content> found = contentRepository.findByUrlAndSource("https://example.com/1", "facebook");
        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getTitle());
    }

    @Test
    void findByStatus_returnsMatchingContent() {
        Content c1 = createContent("Unread", null, "facebook", "unread");
        contentRepository.save(c1);

        Content c2 = createContent("Read", null, "facebook", "completed");
        contentRepository.save(c2);

        assertEquals(1, contentRepository.findByStatus("unread").size());
    }

    @Test
    void uniqueConstraint_preventsDuplicateUrlSource() {
        Content c1 = createContent("First", "https://example.com/dup", "facebook", "unread");
        contentRepository.save(c1);

        Content c2 = createContent("Second", "https://example.com/dup", "facebook", "unread");

        assertThrows(Exception.class, () -> {
            contentRepository.save(c2);
            contentRepository.flush();
        });
    }
}
