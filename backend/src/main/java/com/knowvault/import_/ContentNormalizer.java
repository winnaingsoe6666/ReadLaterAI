package com.knowvault.import_;

import com.knowvault.content.Content;
import com.knowvault.import_.dto.RawContent;
import com.knowvault.tag.Tag;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ContentNormalizer {

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = Map.of(
        "technology", List.of("java", "python", "javascript", "programming", "code", "software", "api", "developer", "tech", "ai", "machine learning"),
        "food", List.of("recipe", "cooking", "food", "restaurant", "meal", "dinner", "lunch", "breakfast", "cake", "bread"),
        "news", List.of("breaking", "report", "news", "update", "announce", "government", "election", "policy"),
        "entertainment", List.of("movie", "music", "game", "show", "concert", "album", "film", "watch"),
        "health", List.of("health", "exercise", "fitness", "diet", "medical", "doctor", "workout", "wellness"),
        "education", List.of("learn", "course", "tutorial", "study", "university", "school", "training", "guide")
    );

    public Content normalize(RawContent raw) {
        Content content = new Content();
        content.setTitle(raw.getTitle() != null ? raw.getTitle() : "");
        content.setContentText(raw.getContentText() != null ? raw.getContentText() : "");
        content.setUrl(raw.getUrl() != null ? raw.getUrl() : "");
        content.setSource("facebook");

        // Extract author from title if not provided
        String author = raw.getAuthor() != null ? raw.getAuthor() : "";
        if (author.isEmpty() && raw.getTitle() != null) {
            author = extractAuthorFromTitle(raw.getTitle());
        }
        content.setAuthor(author);

        content.setCreatedDate(raw.getTimestamp());
        content.setStatus("unread");
        content.setFavorite(false);
        content.setCategory(categorize(raw));
        content.setImportDate(Instant.now().toString());
        return content;
    }

    private String extractAuthorFromTitle(String title) {
        // "Win Naing Soe saved Yin Maung's post." -> "Yin Maung"
        // "Win Naing Soe shared a post." -> ""
        // "Win Naing Soe saved a link from Duwun's post." -> "Duwun"

        if (title == null) return "";

        // Pattern: "saved X's post"
        var savedMatcher = java.util.regex.Pattern.compile("saved (.+?)'s post").matcher(title);
        if (savedMatcher.find()) {
            return savedMatcher.group(1);
        }

        // Pattern: "saved a link from X's post"
        var linkMatcher = java.util.regex.Pattern.compile("saved a link from (.+?)'s post").matcher(title);
        if (linkMatcher.find()) {
            return linkMatcher.group(1);
        }

        // Pattern: "saved a video from X's post"
        var videoMatcher = java.util.regex.Pattern.compile("saved a video from (.+?)'s post").matcher(title);
        if (videoMatcher.find()) {
            return videoMatcher.group(1);
        }

        return "";
    }

    public Set<Tag> extractTags(RawContent raw) {
        String text = ((raw.getTitle() != null ? raw.getTitle() : "") + " " +
                       (raw.getContentText() != null ? raw.getContentText() : "")).toLowerCase();
        Set<String> tagNames = new HashSet<>();

        // Extract words longer than 4 chars as potential tags
        String[] words = text.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        Map<String, Long> wordFreq = Arrays.stream(words)
            .filter(w -> w.length() > 4)
            .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        // Top 5 most frequent words become tags
        wordFreq.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(e -> tagNames.add(e.getKey()));

        // Add source-type tag
        if (raw.getSourceType() != null) {
            tagNames.add(raw.getSourceType());
        }

        // Add data quality tags
        boolean hasUrl = raw.getUrl() != null && !raw.getUrl().isEmpty();
        boolean hasContent = raw.getContentText() != null && !raw.getContentText().isEmpty();

        if (hasUrl) {
            tagNames.add("has-link");
        }
        if (hasContent) {
            tagNames.add("has-content");
        }
        if (!hasUrl && !hasContent) {
            tagNames.add("title-only");
        }

        return tagNames.stream()
            .map(name -> {
                Tag tag = new Tag();
                tag.setName(name);
                return tag;
            })
            .collect(Collectors.toSet());
    }

    public String categorize(RawContent raw) {
        String text = ((raw.getTitle() != null ? raw.getTitle() : "") + " " +
                       (raw.getContentText() != null ? raw.getContentText() : "")).toLowerCase();

        Map<String, Long> scores = new HashMap<>();
        for (var entry : CATEGORY_KEYWORDS.entrySet()) {
            long count = entry.getValue().stream()
                .filter(text::contains)
                .count();
            if (count > 0) {
                scores.put(entry.getKey(), count);
            }
        }

        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("general");
    }
}
