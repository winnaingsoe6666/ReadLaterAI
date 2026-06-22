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

    // ========== Messenger Messages Parser ==========

    public List<RawContent> parseMessages(Path archiveDir) {
        List<RawContent> results = new ArrayList<>();

        // Try new format: your_messenger_activity/messages/inbox/
        Path newInbox = archiveDir.resolve("your_messenger_activity").resolve("messages").resolve("inbox");
        // Try old format: messages/inbox/
        Path oldInbox = archiveDir.resolve("messages").resolve("inbox");

        Path inboxDir = Files.exists(newInbox) ? newInbox : (Files.exists(oldInbox) ? oldInbox : null);
        if (inboxDir == null) {
            System.out.println("[DEBUG] No messenger inbox found");
            return results;
        }

        System.out.println("[DEBUG] Found messenger inbox: " + inboxDir);
        Set<String> seenUrls = new HashSet<>();

        try {
            List<Path> conversations = Files.list(inboxDir)
                .filter(Files::isDirectory)
                .collect(Collectors.toList());
            for (Path convDir : conversations) {
                try {
                    String title = convDir.getFileName().toString();
                    results.addAll(parseConversationMessages(convDir, title, seenUrls));
                } catch (Exception e) {
                    System.out.println("[DEBUG] Skipping conversation due to error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[DEBUG] Failed to read inbox directory: " + e.getMessage());
        }

        System.out.println("[DEBUG] Parsed " + results.size() + " messenger links total");
        return results;
    }

    private List<RawContent> parseConversationMessages(Path dir, String title, Set<String> seenUrls) {
        List<RawContent> results = new ArrayList<>();

        try {
            List<Path> jsonFiles = Files.list(dir)
                .filter(p -> p.getFileName().toString().startsWith("message_") && p.getFileName().toString().endsWith(".json"))
                .sorted()
                .collect(Collectors.toList());

            for (Path jsonFile : jsonFiles) {
                try {
                    String content = Files.readString(jsonFile);
                    Map<String, Object> root = objectMapper.readValue(content, new TypeReference<>() {});

                    String conversationTitle = title;
                    Object titleObj = root.get("title");
                    if (titleObj != null && !titleObj.toString().isEmpty()) {
                        conversationTitle = titleObj.toString();
                    }

                    Object messagesObj = root.get("messages");
                    if (!(messagesObj instanceof List)) continue;
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) messagesObj;

                    for (Map<String, Object> msg : messages) {
                        try {
                            // Skip unsent messages
                            Object isUnsent = msg.get("is_unsent");
                            if (isUnsent != null && Boolean.TRUE.equals(isUnsent)) continue;

                            String senderName = getStringValue(msg, "sender_name");

                            // Extract share link
                            String url = "";
                            String shareText = "";
                            if (msg.containsKey("share")) {
                                Object shareObj = msg.get("share");
                                if (shareObj instanceof Map) {
                                    Map<String, Object> share = (Map<String, Object>) shareObj;
                                    url = getStringValue(share, "link");
                                    shareText = getStringValue(share, "share_text");
                                }
                            }

                            String messageContent = getStringValue(msg, "content");

                            // Skip messages without share link and short content
                            if (url.isEmpty() && messageContent.length() < 100) continue;

                            // Deduplicate by URL
                            if (!url.isEmpty() && seenUrls.contains(url)) continue;
                            if (!url.isEmpty()) seenUrls.add(url);

                            // Convert timestamp_ms
                            String timestamp = "";
                            Object tsObj = msg.get("timestamp_ms");
                            if (tsObj instanceof Number) {
                                timestamp = java.time.Instant.ofEpochMilli(((Number) tsObj).longValue()).toString();
                            }

                            String displayContent = !shareText.isEmpty() ? shareText : messageContent;

                            results.add(RawContent.builder()
                                .title(senderName + " shared in " + conversationTitle)
                                .contentText(displayContent)
                                .url(url)
                                .timestamp(timestamp)
                                .author(senderName)
                                .sourceType(RawContent.TYPE_MESSENGER_MESSAGE)
                                .build());
                        } catch (Exception e) {
                            System.out.println("[DEBUG] Skipping message due to error: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[DEBUG] Skipping message file due to error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[DEBUG] Failed to read conversation directory: " + e.getMessage());
        }

        return results;
    }

    // ========== Groups Parser ==========

    public List<RawContent> parseGroupPosts(Path archiveDir) {
        List<RawContent> results = new ArrayList<>();

        // Try new format: your_facebook_activity/groups/your_posts_in_groups/
        Path newDir = archiveDir.resolve("your_facebook_activity").resolve("groups").resolve("your_posts_in_groups");
        // Try old format: groups/your_posts_in_groups/
        Path oldDir = archiveDir.resolve("groups").resolve("your_posts_in_groups");

        Path groupsDir = Files.exists(newDir) ? newDir : (Files.exists(oldDir) ? oldDir : null);
        if (groupsDir == null) {
            System.out.println("[DEBUG] No group posts directory found");
            return results;
        }

        System.out.println("[DEBUG] Found group posts directory: " + groupsDir);
        try {
            List<Path> jsonFiles = Files.list(groupsDir)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());
            for (Path file : jsonFiles) {
                try {
                    results.addAll(parseGroupPostsJson(file));
                } catch (Exception e) {
                    System.out.println("[DEBUG] Skipping group posts file due to error: " + file.getFileName() + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[DEBUG] Failed to read group posts directory: " + e.getMessage());
        }

        System.out.println("[DEBUG] Parsed " + results.size() + " group posts");
        return results;
    }

    private List<RawContent> parseGroupPostsJson(Path file) throws IOException {
        String content = Files.readString(file);
        List<Map<String, Object>> posts = objectMapper.readValue(content, new TypeReference<>() {});
        List<RawContent> results = new ArrayList<>();

        for (Map<String, Object> post : posts) {
            try {
                String title = getStringValue(post, "title");
                String timestamp = getTimestampValue(post);

                String url = extractUrlFromAttachments(post);
                String contentText = extractContentFromData(post);

                if (title != null && !title.isEmpty()) {
                    results.add(RawContent.builder()
                        .title(title)
                        .contentText(contentText)
                        .url(url)
                        .timestamp(timestamp)
                        .author("")
                        .sourceType(RawContent.TYPE_GROUP_POST)
                        .build());
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Skipping individual group post: " + e.getMessage());
            }
        }
        return results;
    }

    public List<RawContent> parseGroupCommentedPosts(Path archiveDir) {
        List<RawContent> results = new ArrayList<>();

        Path newDir = archiveDir.resolve("your_facebook_activity").resolve("groups").resolve("posts_commented_on_in_groups");
        Path oldDir = archiveDir.resolve("groups").resolve("posts_commented_on_in_groups");

        Path groupsDir = Files.exists(newDir) ? newDir : (Files.exists(oldDir) ? oldDir : null);
        if (groupsDir == null) {
            System.out.println("[DEBUG] No group commented posts directory found");
            return results;
        }

        System.out.println("[DEBUG] Found group commented posts directory: " + groupsDir);
        try {
            List<Path> jsonFiles = Files.list(groupsDir)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());
            for (Path file : jsonFiles) {
                try {
                    String fileContent = Files.readString(file);
                    List<Map<String, Object>> posts = objectMapper.readValue(fileContent, new TypeReference<>() {});
                    for (Map<String, Object> post : posts) {
                        try {
                            String title = getStringValue(post, "title");
                            String timestamp = getTimestampValue(post);
                            String url = extractUrlFromAttachments(post);
                            String contentText = extractContentFromData(post);

                            if (title != null && !title.isEmpty()) {
                                results.add(RawContent.builder()
                                    .title(title)
                                    .contentText(contentText)
                                    .url(url)
                                    .timestamp(timestamp)
                                    .author("")
                                    .sourceType(RawContent.TYPE_GROUP_COMMENTED_POST)
                                    .build());
                            }
                        } catch (Exception e) {
                            System.out.println("[DEBUG] Skipping individual commented post: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[DEBUG] Skipping commented posts file: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[DEBUG] Failed to read commented posts directory: " + e.getMessage());
        }

        System.out.println("[DEBUG] Parsed " + results.size() + " group commented posts");
        return results;
    }

    public List<RawContent> parseGroupComments(Path archiveDir) {
        List<RawContent> results = new ArrayList<>();

        Path newDir = archiveDir.resolve("your_facebook_activity").resolve("groups").resolve("your_comments_in_groups");
        Path oldDir = archiveDir.resolve("groups").resolve("your_comments_in_groups");

        Path groupsDir = Files.exists(newDir) ? newDir : (Files.exists(oldDir) ? oldDir : null);
        if (groupsDir == null) {
            System.out.println("[DEBUG] No group comments directory found");
            return results;
        }

        System.out.println("[DEBUG] Found group comments directory: " + groupsDir);
        try {
            List<Path> jsonFiles = Files.list(groupsDir)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());
            for (Path file : jsonFiles) {
                try {
                    String fileContent = Files.readString(file);
                    List<Map<String, Object>> items = objectMapper.readValue(fileContent, new TypeReference<>() {});
                    for (Map<String, Object> item : items) {
                        try {
                            String title = getStringValue(item, "title");
                            String timestamp = getTimestampValue(item);
                            String url = extractUrlFromAttachments(item);
                            String contentText = extractContentFromData(item);

                            if (title != null && !title.isEmpty()) {
                                results.add(RawContent.builder()
                                    .title(title)
                                    .contentText(contentText)
                                    .url(url)
                                    .timestamp(timestamp)
                                    .author("")
                                    .sourceType(RawContent.TYPE_COMMENT)
                                    .build());
                            }
                        } catch (Exception e) {
                            System.out.println("[DEBUG] Skipping individual group comment: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[DEBUG] Skipping group comments file: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[DEBUG] Failed to read group comments directory: " + e.getMessage());
        }

        System.out.println("[DEBUG] Parsed " + results.size() + " group comments");
        return results;
    }

    // ========== Comments Parser ==========

    public List<RawContent> parseComments(Path archiveDir) {
        List<RawContent> results = new ArrayList<>();

        // Try new format: your_facebook_activity/comments/
        Path newDir = archiveDir.resolve("your_facebook_activity").resolve("comments");
        // Try old format: comments/
        Path oldDir = archiveDir.resolve("comments");

        Path commentsDir = Files.exists(newDir) ? newDir : (Files.exists(oldDir) ? oldDir : null);
        if (commentsDir == null) {
            System.out.println("[DEBUG] No comments directory found");
            return results;
        }

        System.out.println("[DEBUG] Found comments directory: " + commentsDir);
        try {
            List<Path> jsonFiles = Files.list(commentsDir)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .collect(Collectors.toList());
            for (Path file : jsonFiles) {
                try {
                    results.addAll(parseCommentsJson(file));
                } catch (Exception e) {
                    System.out.println("[DEBUG] Skipping comments file due to error: " + file.getFileName() + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("[DEBUG] Failed to read comments directory: " + e.getMessage());
        }

        System.out.println("[DEBUG] Parsed " + results.size() + " comments");
        return results;
    }

    private List<RawContent> parseCommentsJson(Path file) throws IOException {
        String content = Files.readString(file);
        List<Map<String, Object>> entries = objectMapper.readValue(content, new TypeReference<>() {});
        List<RawContent> results = new ArrayList<>();

        for (Map<String, Object> entry : entries) {
            try {
                String timestamp = "";
                Object tsObj = entry.get("timestamp");
                if (tsObj instanceof Number) {
                    long unixTs = ((Number) tsObj).longValue();
                    if (unixTs > 10000000000L) {
                        timestamp = java.time.Instant.ofEpochMilli(unixTs).toString();
                    } else {
                        timestamp = java.time.Instant.ofEpochSecond(unixTs).toString();
                    }
                }

                // Parse nested data array
                Object dataObj = entry.get("data");
                if (!(dataObj instanceof List)) continue;
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;

                for (Map<String, Object> data : dataList) {
                    try {
                        Object commentObj = data.get("comment");
                        if (!(commentObj instanceof Map)) continue;
                        Map<String, Object> comment = (Map<String, Object>) commentObj;

                        String commentTitle = getStringValue(comment, "title");
                        String commentText = getStringValue(comment, "comment");
                        String author = getStringValue(comment, "author");

                        String url = "";
                        if (comment.containsKey("attachment")) {
                            Object attachObj = comment.get("attachment");
                            if (attachObj instanceof Map) {
                                Map<String, Object> attachment = (Map<String, Object>) attachObj;
                                url = getStringValue(attachment, "uri");
                            }
                        }

                        if (commentTitle.isEmpty() && commentText.isEmpty()) continue;

                        results.add(RawContent.builder()
                            .title(commentTitle)
                            .contentText(commentText)
                            .url(url)
                            .timestamp(timestamp)
                            .author(author)
                            .sourceType(RawContent.TYPE_COMMENT)
                            .build());
                    } catch (Exception e) {
                        System.out.println("[DEBUG] Skipping individual comment entry: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("[DEBUG] Skipping comment block: " + e.getMessage());
            }
        }
        return results;
    }

    // ========== Interest Metadata Parser ==========

    public List<RawContent> parseLikedPages(Path archiveDir) {
        List<RawContent> results = new ArrayList<>();

        // Try multiple paths
        Path[] candidates = {
            archiveDir.resolve("your_facebook_activity").resolve("likes_and_reactions").resolve("pages.json"),
            archiveDir.resolve("likes_and_reactions").resolve("pages.json")
        };

        Path pagesFile = null;
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                pagesFile = candidate;
                break;
            }
        }
        if (pagesFile == null) {
            System.out.println("[DEBUG] No liked pages file found");
            return results;
        }

        System.out.println("[DEBUG] Found liked pages file: " + pagesFile);
        try {
            String content = Files.readString(pagesFile);
            Map<String, Object> root = objectMapper.readValue(content, new TypeReference<>() {});

            Object pagesObj = root.get("pages_v2");
            if (pagesObj instanceof List) {
                List<Map<String, Object>> pages = (List<Map<String, Object>>) pagesObj;
                for (Map<String, Object> page : pages) {
                    try {
                        String name = getStringValue(page, "name");
                        String timestamp = "";
                        Object tsObj = page.get("timestamp");
                        if (tsObj instanceof Number) {
                            long unixTs = ((Number) tsObj).longValue();
                            timestamp = unixTs > 10000000000L
                                ? java.time.Instant.ofEpochMilli(unixTs).toString()
                                : java.time.Instant.ofEpochSecond(unixTs).toString();
                        }

                        if (!name.isEmpty()) {
                            results.add(RawContent.builder()
                                .title(name)
                                .contentText("")
                                .url("")
                                .timestamp(timestamp)
                                .author("")
                                .sourceType(RawContent.TYPE_LIKED_PAGE)
                                .metadata(Map.of("page_name", name))
                                .build());
                        }
                    } catch (Exception e) {
                        System.out.println("[DEBUG] Skipping individual liked page: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[DEBUG] Failed to parse liked pages: " + e.getMessage());
        }

        System.out.println("[DEBUG] Parsed " + results.size() + " liked pages");
        return results;
    }

    public List<RawContent> parseAdPreferences(Path archiveDir) {
        List<RawContent> results = new ArrayList<>();

        Path[] candidates = {
            archiveDir.resolve("your_facebook_activity").resolve("ads_and_businesses").resolve("your_ad_preferences.json"),
            archiveDir.resolve("ads_information").resolve("your_ad_preferences.json")
        };

        Path prefsFile = null;
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                prefsFile = candidate;
                break;
            }
        }
        if (prefsFile == null) {
            System.out.println("[DEBUG] No ad preferences file found");
            return results;
        }

        System.out.println("[DEBUG] Found ad preferences file: " + prefsFile);
        try {
            String content = Files.readString(prefsFile);
            Map<String, Object> root = objectMapper.readValue(content, new TypeReference<>() {});

            // Parse topics
            Object topicsObj = root.get("topics");
            if (topicsObj instanceof List) {
                List<Map<String, Object>> topics = (List<Map<String, Object>>) topicsObj;
                for (Map<String, Object> topic : topics) {
                    String name = getStringValue(topic, "name");
                    if (!name.isEmpty()) {
                        results.add(RawContent.builder()
                            .title(name)
                            .contentText("")
                            .url("")
                            .timestamp("")
                            .author("")
                            .sourceType(RawContent.TYPE_AD_PREFERENCE)
                            .metadata(Map.of("preference_type", "topic"))
                            .build());
                    }
                }
            }

            // Parse interests
            Object interestsObj = root.get("interests");
            if (interestsObj instanceof List) {
                List<Map<String, Object>> interests = (List<Map<String, Object>>) interestsObj;
                for (Map<String, Object> interest : interests) {
                    String name = getStringValue(interest, "name");
                    if (!name.isEmpty()) {
                        results.add(RawContent.builder()
                            .title(name)
                            .contentText("")
                            .url("")
                            .timestamp("")
                            .author("")
                            .sourceType(RawContent.TYPE_AD_PREFERENCE)
                            .metadata(Map.of("preference_type", "interest"))
                            .build());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[DEBUG] Failed to parse ad preferences: " + e.getMessage());
        }

        System.out.println("[DEBUG] Parsed " + results.size() + " ad preferences");
        return results;
    }

    // Helper: extract URL from attachments[0].data[0].external_context.url
    private String extractUrlFromAttachments(Map<String, Object> post) {
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
                                        return getStringValue(externalContext, "url");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    // Helper: extract content from data[0].post
    private String extractContentFromData(Map<String, Object> post) {
        if (post.containsKey("data")) {
            Object dataFieldObj = post.get("data");
            if (dataFieldObj instanceof List) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataFieldObj;
                for (Map<String, Object> data : dataList) {
                    if (data.containsKey("post")) {
                        return getStringValue(data, "post");
                    }
                }
            }
        }
        return "";
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
