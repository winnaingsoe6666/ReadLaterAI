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
        Path postsDir = archiveDir.resolve("posts");
        if (!Files.exists(postsDir)) {
            return Collections.emptyList();
        }
        try {
            List<RawContent> results = new ArrayList<>();
            Files.list(postsDir)
                .filter(p -> p.toString().endsWith(".html"))
                .forEach(file -> {
                    try {
                        results.addAll(parseHtmlPosts(file));
                    } catch (IOException e) {
                        throw new ImportException("Failed to parse posts file: " + file, e);
                    }
                });
            return results;
        } catch (IOException e) {
            throw new ImportException("Failed to read posts directory", e);
        }
    }

    public List<RawContent> parseSavedItems(Path archiveDir) {
        Path savedDir = archiveDir.resolve("saved_items");
        if (!Files.exists(savedDir)) {
            return Collections.emptyList();
        }
        try {
            Path jsonFile = Files.list(savedDir)
                .filter(p -> p.toString().endsWith(".json"))
                .findFirst()
                .orElse(null);
            if (jsonFile == null) return Collections.emptyList();

            String content = Files.readString(jsonFile);
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
        } catch (IOException e) {
            throw new ImportException("Failed to parse saved items", e);
        }
    }

    public void cleanup(Path tempDir) {
        try {
            if (tempDir != null && Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
            }
        } catch (IOException ignored) {}
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
}
