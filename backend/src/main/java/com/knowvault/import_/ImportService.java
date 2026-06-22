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
            int postCount = 0, savedCount = 0, messengerCount = 0, groupCount = 0, commentCount = 0, metadataCount = 0;

            try {
                List<RawContent> posts = parser.parsePosts(tempDir);
                postCount = posts.size();
                allRaw.addAll(posts);
            } catch (Exception e) {
                log.warn("Failed to parse posts: {}", e.getMessage());
            }
            try {
                List<RawContent> saved = parser.parseSavedItems(tempDir);
                savedCount = saved.size();
                allRaw.addAll(saved);
            } catch (Exception e) {
                log.warn("Failed to parse saved items: {}", e.getMessage());
            }
            try {
                List<RawContent> messenger = parser.parseMessages(tempDir);
                messengerCount = messenger.size();
                allRaw.addAll(messenger);
            } catch (Exception e) {
                log.warn("Failed to parse messenger: {}", e.getMessage());
            }
            try {
                List<RawContent> groupPosts = parser.parseGroupPosts(tempDir);
                List<RawContent> groupCommented = parser.parseGroupCommentedPosts(tempDir);
                List<RawContent> groupComments = parser.parseGroupComments(tempDir);
                groupCount = groupPosts.size() + groupCommented.size() + groupComments.size();
                allRaw.addAll(groupPosts);
                allRaw.addAll(groupCommented);
                allRaw.addAll(groupComments);
            } catch (Exception e) {
                log.warn("Failed to parse groups: {}", e.getMessage());
            }
            try {
                List<RawContent> comments = parser.parseComments(tempDir);
                commentCount = comments.size();
                allRaw.addAll(comments);
            } catch (Exception e) {
                log.warn("Failed to parse comments: {}", e.getMessage());
            }
            try {
                List<RawContent> likedPages = parser.parseLikedPages(tempDir);
                List<RawContent> adPrefs = parser.parseAdPreferences(tempDir);
                metadataCount = likedPages.size() + adPrefs.size();
                allRaw.addAll(likedPages);
                allRaw.addAll(adPrefs);
            } catch (Exception e) {
                log.warn("Failed to parse metadata: {}", e.getMessage());
            }

            log.info("Parsed: {} posts, {} saved, {} messenger, {} groups, {} comments, {} metadata",
                    postCount, savedCount, messengerCount, groupCount, commentCount, metadataCount);

            for (RawContent raw : allRaw) {
                try {
                    boolean wasImported = importSingleItem(raw);
                    if (wasImported) {
                        imported++;
                    } else {
                        skipped++;
                    }
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
    public boolean importSingleItem(RawContent raw) {
        // Skip metadata types (liked pages, ad preferences)
        if (normalizer.isMetadata(raw.getSourceType())) {
            log.debug("Skipping metadata item: {}", raw.getTitle());
            return false;
        }

        // Check for duplicates
        if (raw.getUrl() != null && !raw.getUrl().isEmpty()) {
            Optional<Content> existing = contentRepository.findByUrlAndSource(raw.getUrl(), "facebook");
            if (existing.isPresent()) {
                return false;
            }
        }

        Content content = normalizer.normalize(raw);
        Set<Tag> tags = normalizer.extractTags(raw);

        // Resolve tags - reuse existing ones
        Set<Tag> resolvedTags = resolveTags(tags);
        content.setTags(resolvedTags);

        try {
            contentRepository.save(content);
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Duplicate URL+source from concurrent/overlapping data
            log.debug("Duplicate skipped: {}", raw.getTitle());
            return false;
        }
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
