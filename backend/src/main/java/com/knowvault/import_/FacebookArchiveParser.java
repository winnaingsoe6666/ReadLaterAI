package com.knowvault.import_;

import com.knowvault.common.exception.ImportException;
import com.knowvault.import_.dto.RawContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class FacebookArchiveParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImportException("Uploaded file is empty");
        }
        if (!file.getOriginalFilename().endsWith(".zip")) {
            throw new ImportException("File must be a ZIP archive");
        }
    }

    public Path extract(MultipartFile file) {
        try {
            Path tempDir = Files.createTempDirectory("knowvault-import-");
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path entryPath = tempDir.resolve(entry.getName());
                    if (!entryPath.normalize().startsWith(tempDir.normalize())) {
                        throw new ImportException("Zip entry path escapes target directory: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }
            return tempDir;
        } catch (IOException e) {
            throw new ImportException("Failed to extract ZIP archive", e);
        }
    }

    public List<RawContent> parsePosts(Path archiveDir) {
        List<RawContent> results = new ArrayList<>();

        // Try your_facebook_activity/posts/ (both JSON and HTML)
        Path newPostsDir = archiveDir.resolve("your_facebook_activity").resolve("posts");
        if (Files.exists(newPostsDir)) {
            System.out.println("[DEBUG] Found posts directory: " + newPostsDir);
            try {
                List<Path> files = Files.list(newPostsDir).collect(Collectors.toList());
                for (Path file : files) {
                    try {
                        String fileName = file.getFileName().toString();
                        if (fileName.endsWith(".json") && fileName.startsWith("your_posts__")) {
                            results.addAll(parseJsonPosts(file));
                        } else if (fileName.endsWith(".html") && fileName.startsWith("your_posts__")) {
                            results.addAll(parseNewHtmlPosts(file));
                        }
                    } catch (Exception e) {
                        System.out.println("[DEBUG] Skipping file due to error: " + file.getFileName() + " - " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                throw new ImportException("Failed to read posts directory", e);
            }
            if (!results.isEmpty()) return results;
        }

        // Try old format: posts/*.html
        Path oldPostsDir = archiveDir.resolve("posts");
        if (Files.exists(oldPostsDir)) {
            System.out.println("[DEBUG] Found old format posts directory: " + oldPostsDir);
            try {
                List<Path> files = Files.list(oldPostsDir)
                    .filter(p -> p.toString().endsWith(".html"))
                    .collect(Collectors.toList());
                for (Path file : files) {
                    try {
                        results.addAll(parseHtmlPosts(file));
                    } catch (Exception e) {
                        System.out.println("[DEBUG] Skipping file due to error: " + file.getFileName() + " - " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                throw new ImportException("Failed to read posts directory", e);
            }
        }

        return results;
    }

    public List<RawContent> parseSavedItems(Path archiveDir) {
        List<RawContent> results = new ArrayList<>();

        // Try your_facebook_activity/saved_items_and_collections/
        Path newSavedDir = archiveDir.resolve("your_facebook_activity").resolve("saved_items_and_collections");
        if (Files.exists(newSavedDir)) {
            System.out.println("[DEBUG] Found saved items directory: " + newSavedDir);
            try {
                // Try JSON first
                Path jsonFile = Files.list(newSavedDir)
                    .filter(p -> p.getFileName().toString().equals("your_saved_items.json"))
                    .findFirst()
                    .orElse(null);
                if (jsonFile != null) {
                    results.addAll(parseNewSavedItems(jsonFile));
                    if (!results.isEmpty()) return results;
                }

                // Try HTML format
                Path htmlFile = Files.list(newSavedDir)
                    .filter(p -> p.getFileName().toString().equals("your_saved_items.html"))
                    .findFirst()
                    .orElse(null);
                if (htmlFile != null) {
                    results.addAll(parseSavedItemsHtml(htmlFile));
                    if (!results.isEmpty()) return results;
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Skipping saved items due to error: " + e.getMessage());
            }
        }

        // Try old format: saved_items/*.json
        Path oldSavedDir = archiveDir.resolve("saved_items");
        if (Files.exists(oldSavedDir)) {
            System.out.println("[DEBUG] Found old format saved items directory: " + oldSavedDir);
            try {
                Path jsonFile = Files.list(oldSavedDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .findFirst()
                    .orElse(null);
                if (jsonFile != null) {
                    results.addAll(parseOldSavedItems(jsonFile));
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Skipping saved items due to error: " + e.getMessage());
            }
        }

        return results;
    }

    public void cleanup(Path tempDir) {
        // DEBUG: Keep temp files for debugging - comment out cleanup
        System.out.println("[DEBUG] Temp directory preserved at: " + tempDir);
        // try {
        //     if (tempDir != null && Files.exists(tempDir)) {
        //         Files.walk(tempDir)
        //             .sorted(Comparator.reverseOrder())
        //             .forEach(path -> {
        //                 try { Files.deleteIfExists(path); } catch (IOException ignored) {}
        //             });
        //     }
        // } catch (IOException ignored) {}
    }

    // Parse new JSON format posts
    private List<RawContent> parseJsonPosts(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        System.out.println("[DEBUG] Parsing JSON posts file: " + fileName);

        // Only parse the main posts file, skip other files with different structures
        if (!fileName.startsWith("your_posts__")) {
            System.out.println("[DEBUG] Skipping non-posts file: " + fileName);
            return new ArrayList<>();
        }

        String content = Files.readString(file);
        List<Map<String, Object>> posts = objectMapper.readValue(content, new TypeReference<>() {});

        List<RawContent> results = new ArrayList<>();
        for (Map<String, Object> post : posts) {
            try {
                String title = getStringValue(post, "title");
                String timestamp = getTimestampValue(post);

                // Try to get URL from attachments
                String url = "";
                if (post.containsKey("attachments")) {
                    Object attachmentsObj = post.get("attachments");
                    if (attachmentsObj instanceof List) {
                        List<Map<String, Object>> attachments = (List<Map<String, Object>>) attachmentsObj;
                        if (!attachments.isEmpty()) {
                            Map<String, Object> attachment = attachments.get(0);
                            if (attachment.containsKey("data")) {
                                Object dataObj = attachment.get("data");
                                if (dataObj instanceof List) {
                                    List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
                                    if (!dataList.isEmpty()) {
                                        Map<String, Object> data = dataList.get(0);
                                        if (data.containsKey("external_context")) {
                                            Object extObj = data.get("external_context");
                                            if (extObj instanceof Map) {
                                                Map<String, Object> externalContext = (Map<String, Object>) extObj;
                                                url = getStringValue(externalContext, "url");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Try to get content from data field
                String contentText = "";
                if (post.containsKey("data")) {
                    Object dataFieldObj = post.get("data");
                    if (dataFieldObj instanceof List) {
                        List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataFieldObj;
                        for (Map<String, Object> data : dataList) {
                            if (data.containsKey("post")) {
                                contentText = getStringValue(data, "post");
                                break;
                            }
                        }
                    }
                }

                if (title != null && !title.isEmpty()) {
                    results.add(RawContent.builder()
                        .title(title)
                        .contentText(contentText)
                        .url(url)
                        .timestamp(timestamp)
                        .author("")
                        .sourceType("post")
                        .build());
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Skipping individual post due to error: " + e.getMessage());
            }
        }
        System.out.println("[DEBUG] Parsed " + results.size() + " posts from " + fileName);
        return results;
    }

    // Parse new format saved items
    private List<RawContent> parseNewSavedItems(Path file) throws IOException {
        System.out.println("[DEBUG] Parsing new format saved items: " + file.getFileName());
        String content = Files.readString(file);
        Map<String, Object> root = objectMapper.readValue(content, new TypeReference<>() {});

        List<RawContent> results = new ArrayList<>();
        if (root.containsKey("saves_v2")) {
            Object savesObj = root.get("saves_v2");
            if (savesObj instanceof List) {
                List<Map<String, Object>> saves = (List<Map<String, Object>>) savesObj;
                for (Map<String, Object> save : saves) {
                    try {
                        String title = getStringValue(save, "title");
                        String timestamp = getTimestampValue(save);

                        // Try to get URL from attachments
                        String url = "";
                        if (save.containsKey("attachments")) {
                            Object attachmentsObj = save.get("attachments");
                            if (attachmentsObj instanceof List) {
                                List<Map<String, Object>> attachments = (List<Map<String, Object>>) attachmentsObj;
                                if (!attachments.isEmpty()) {
                                    Map<String, Object> attachment = attachments.get(0);
                                    if (attachment.containsKey("data")) {
                                        Object dataObj = attachment.get("data");
                                        if (dataObj instanceof List) {
                                            List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
                                            if (!dataList.isEmpty()) {
                                                Map<String, Object> data = dataList.get(0);
                                                if (data.containsKey("external_context")) {
                                                    Object extObj = data.get("external_context");
                                                    if (extObj instanceof Map) {
                                                        Map<String, Object> externalContext = (Map<String, Object>) extObj;
                                                        url = getStringValue(externalContext, "source");
                                                        if (url.isEmpty()) {
                                                            url = getStringValue(externalContext, "url");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (title != null && !title.isEmpty()) {
                            results.add(RawContent.builder()
                                .title(title)
                                .contentText("")
                                .url(url)
                                .timestamp(timestamp)
                                .author("")
                                .sourceType("saved_item")
                                .build());
                        }
                    } catch (Exception e) {
                        System.out.println("[DEBUG] Skipping individual saved item due to error: " + e.getMessage());
                    }
                }
            }
        }
        System.out.println("[DEBUG] Parsed " + results.size() + " saved items from " + file.getFileName());
        return results;
    }

    // Parse old format saved items
    private List<RawContent> parseOldSavedItems(Path file) throws IOException {
        String content = Files.readString(file);
        List<Map<String, String>> items = objectMapper.readValue(content, new TypeReference<>() {});
        return items.stream()
            .map(item -> RawContent.builder()
                .title(item.getOrDefault("title", ""))
                .contentText(item.getOrDefault("content", ""))
                .url(item.getOrDefault("url", ""))
                .timestamp(item.getOrDefault("timestamp", ""))
                .author("")
                .sourceType("saved_item")
                .build())
            .collect(Collectors.toList());
    }

    private List<RawContent> parseHtmlPosts(Path file) throws IOException {
        Document doc = Jsoup.parse(file.toFile(), "UTF-8");
        Elements posts = doc.select("div.post");
        List<RawContent> results = new ArrayList<>();
        for (Element post : posts) {
            String title = post.selectFirst(".title") != null ? post.selectFirst(".title").text() : "";
            String text = post.selectFirst(".content") != null ? post.selectFirst(".content").text() : "";
            String timestamp = post.selectFirst(".timestamp") != null ? post.selectFirst(".timestamp").text() : "";
            Element link = post.selectFirst("a[href]");
            String url = link != null ? link.attr("href") : "";
            results.add(RawContent.builder()
                .title(title)
                .contentText(text)
                .url(url)
                .timestamp(timestamp)
                .author("")
                .sourceType("post")
                .build());
        }
        return results;
    }

    // Parse new Facebook HTML format (section._a6-g structure)
    private List<RawContent> parseNewHtmlPosts(Path file) throws IOException {
        System.out.println("[DEBUG] Parsing new HTML posts file: " + file.getFileName());
        Document doc = Jsoup.parse(file.toFile(), "UTF-8");
        Elements sections = doc.select("section._a6-g");
        List<RawContent> results = new ArrayList<>();

        for (Element section : sections) {
            try {
                // Title is in h2
                Element titleEl = section.selectFirst("h2");
                String title = titleEl != null ? titleEl.text() : "";

                // Timestamp is in div._a72d
                Element timeEl = section.selectFirst("div._a72d");
                String timestamp = timeEl != null ? timeEl.text() : "";

                // Link is in footer a[href]
                Element linkEl = section.selectFirst("footer a[href]");
                String url = linkEl != null ? linkEl.attr("href") : "";

                // Content text - look for post content
                Element contentEl = section.selectFirst("div._2ph_._a6-p");
                String contentText = "";
                if (contentEl != null) {
                    // Get text content, excluding nested divs with timestamps
                    contentText = contentEl.text();
                    // Clean up if it contains timestamp
                    if (timestamp != null && !timestamp.isEmpty() && contentText.contains(timestamp)) {
                        contentText = contentText.replace(timestamp, "").trim();
                    }
                }

                if (!title.isEmpty()) {
                    results.add(RawContent.builder()
                        .title(title)
                        .contentText(contentText)
                        .url(url)
                        .timestamp(timestamp)
                        .author("")
                        .sourceType("post")
                        .build());
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Skipping HTML post section: " + e.getMessage());
            }
        }
        System.out.println("[DEBUG] Parsed " + results.size() + " posts from " + file.getFileName());
        return results;
    }

    // Parse saved items HTML format
    private List<RawContent> parseSavedItemsHtml(Path file) throws IOException {
        System.out.println("[DEBUG] Parsing saved items HTML file: " + file.getFileName());
        Document doc = Jsoup.parse(file.toFile(), "UTF-8");
        Elements sections = doc.select("section._a6-g");
        List<RawContent> results = new ArrayList<>();

        for (Element section : sections) {
            try {
                Element titleEl = section.selectFirst("h2");
                String title = titleEl != null ? titleEl.text() : "";

                Element timeEl = section.selectFirst("div._a72d");
                String timestamp = timeEl != null ? timeEl.text() : "";

                Element linkEl = section.selectFirst("footer a[href]");
                String url = linkEl != null ? linkEl.attr("href") : "";

                if (!title.isEmpty()) {
                    results.add(RawContent.builder()
                        .title(title)
                        .contentText("")
                        .url(url)
                        .timestamp(timestamp)
                        .author("")
                        .sourceType("saved_item")
                        .build());
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Skipping HTML saved item: " + e.getMessage());
            }
        }
        System.out.println("[DEBUG] Parsed " + results.size() + " saved items from " + file.getFileName());
        return results;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private String getTimestampValue(Map<String, Object> map) {
        Object timestamp = map.get("timestamp");
        if (timestamp == null) return "";
        if (timestamp instanceof Number) {
            // Convert Unix timestamp to ISO date string
            long unixTimestamp = ((Number) timestamp).longValue();
            if (unixTimestamp > 10000000000L) {
                // Milliseconds
                return java.time.Instant.ofEpochMilli(unixTimestamp).toString();
            } else {
                // Seconds
                return java.time.Instant.ofEpochSecond(unixTimestamp).toString();
            }
        }
        return timestamp.toString();
    }
}
