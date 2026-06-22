package com.knowvault.import_;

import com.knowvault.common.exception.ImportException;
import com.knowvault.content.Content;
import com.knowvault.content.ContentRepository;
import com.knowvault.import_.dto.ImportResult;
import com.knowvault.import_.dto.RawContent;
import com.knowvault.tag.Tag;
import com.knowvault.tag.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.*;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final FacebookArchiveParser parser;
    private final ContentNormalizer normalizer;
    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;

    public ImportService(FacebookArchiveParser parser, ContentNormalizer normalizer,
                         ContentRepository contentRepository, TagRepository tagRepository) {
        this.parser = parser;
        this.normalizer = normalizer;
        this.contentRepository = contentRepository;
        this.tagRepository = tagRepository;
    }

    public ImportResult importFacebookArchive(MultipartFile file) {
        parser.validate(file);

        Path tempDir = null;
        int imported = 0;
        int skipped = 0;

        try {
            tempDir = parser.extract(file);

            List<RawContent> allRaw = new ArrayList<>();
            try {
                allRaw.addAll(parser.parsePosts(tempDir));
            } catch (Exception e) {
                log.warn("Failed to parse posts: {}", e.getMessage());
            }
            try {
                allRaw.addAll(parser.parseSavedItems(tempDir));
            } catch (Exception e) {
                log.warn("Failed to parse saved items: {}", e.getMessage());
            }

            log.info("Total raw items found: {}", allRaw.size());

            for (RawContent raw : allRaw) {
                try {
                    importSingleItem(raw);
                    imported++;
                } catch (Exception e) {
                    log.warn("Failed to import item '{}': {}", raw.getTitle(), e.getMessage());
                    skipped++;
                }
            }

            return new ImportResult(imported, skipped, imported + skipped);
        } catch (ImportException e) {
            throw e;
        } catch (Exception e) {
            throw new ImportException("Import failed: " + e.getMessage(), e);
        } finally {
            parser.cleanup(tempDir);
        }
    }

    @Transactional
    public void importSingleItem(RawContent raw) {
        // Check for duplicates
        if (raw.getUrl() != null && !raw.getUrl().isEmpty()) {
            Optional<Content> existing = contentRepository.findByUrlAndSource(raw.getUrl(), "facebook");
            if (existing.isPresent()) {
                return;
            }
        }

        Content content = normalizer.normalize(raw);
        Set<Tag> tags = normalizer.extractTags(raw);

        // Resolve tags - reuse existing ones
        Set<Tag> resolvedTags = resolveTags(tags);
        content.setTags(resolvedTags);

        contentRepository.save(content);
    }

    private Set<Tag> resolveTags(Set<Tag> tags) {
        Set<Tag> resolved = new HashSet<>();
        for (Tag tag : tags) {
            Optional<Tag> existing = tagRepository.findByName(tag.getName());
            if (existing.isPresent()) {
                resolved.add(existing.get());
            } else {
                Tag saved = tagRepository.save(tag);
                resolved.add(saved);
            }
        }
        return resolved;
    }
}
