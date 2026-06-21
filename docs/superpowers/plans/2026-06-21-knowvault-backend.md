# KnowVault Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the KnowVault MVP backend — SQLite database schema, JPA entities, repositories, and Facebook archive import module.

**Architecture:** Feature-based modular monolith using Spring Boot 3.3.0 with SQLite. The import module parses Facebook ZIP archives, normalizes content, and persists to the database with duplicate detection.

**Tech Stack:** Java 21, Spring Boot 3.3.0, SQLite (sqlite-jdbc + Hibernate community dialect), Flyway, Spring Data JPA, Lombok

## Global Constraints

- Java 21 minimum
- Spring Boot 3.3.0
- SQLite via sqlite-jdbc 3.45.3.0
- Hibernate community dialects for SQLite
- Flyway for all schema changes
- `ddl-auto: validate` — no auto-generated DDL
- All dates stored as ISO 8601 TEXT in SQLite
- Server port: 8080
- SQLite DB location: `./data/knowvault.db`
- Temp dir for imports: `./data/temp`

---

### Task 1: Scaffold Spring Boot Project

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/knowvault/KnowVaultApplication.java`
- Create: `backend/src/main/resources/application.yml`

**Interfaces:**
- Produces: Maven project structure with all dependencies, runnable Spring Boot app

- [ ] **Step 1: Create pom.xml**

Create `backend/pom.xml` with Spring Boot 3.3.0 parent, dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, sqlite-jdbc 3.45.3.0, hibernate-community-dialects, flyway-core, spring-boot-starter-validation, lombok.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
    </parent>
    <groupId>com.knowvault</groupId>
    <artifactId>knowvault-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>knowvault-backend</name>
    <properties>
        <java.version>21</java.version>
    </properties>
    <dependencies>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
        <dependency><groupId>org.xerial</groupId><artifactId>sqlite-jdbc</artifactId><version>3.45.3.0</version></dependency>
        <dependency><groupId>org.hibernate.orm</groupId><artifactId>hibernate-community-dialects</artifactId></dependency>
        <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId>
                <configuration><excludes><exclude><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></exclude></excludes></configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create application.yml**

Create `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data/knowvault.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    database-platform: org.hibernate.community.dialect.SQLiteDialect
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB

server:
  port: 8080

knowvault:
  import:
    temp-dir: ./data/temp
    max-file-size: 524288000
```

- [ ] **Step 3: Create main application class**

Create `backend/src/main/java/com/knowvault/KnowVaultApplication.java`:

```java
package com.knowvault;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KnowVaultApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowVaultApplication.class, args);
    }
}
```

- [ ] **Step 4: Create data directories**

Run: `mkdir -p backend/data`

- [ ] **Step 5: Verify project compiles**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/
git commit -m "feat: scaffold Spring Boot project with SQLite and Flyway dependencies"
```

---

### Task 2: Database Migrations

**Files:**
- Create: `backend/src/main/resources/db/migration/V1__create_content_table.sql`
- Create: `backend/src/main/resources/db/migration/V2__create_tags_table.sql`
- Create: `backend/src/main/resources/db/migration/V3__create_content_tags_table.sql`
- Create: `backend/src/main/resources/db/migration/V4__create_fts_index.sql`

**Interfaces:**
- Produces: Four tables (content, tags, content_tags, content_fts) with indexes and triggers

- [ ] **Step 1: Create V1 migration**

Create `backend/src/main/resources/db/migration/V1__create_content_table.sql`:

```sql
CREATE TABLE content (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT,
    content_text TEXT,
    url TEXT,
    source TEXT NOT NULL DEFAULT 'facebook',
    category TEXT,
    author TEXT,
    created_date TEXT,
    import_date TEXT NOT NULL DEFAULT (datetime('now')),
    status TEXT NOT NULL DEFAULT 'unread',
    favorite INTEGER NOT NULL DEFAULT 0,
    UNIQUE(url, source)
);

CREATE INDEX idx_content_source ON content(source);
CREATE INDEX idx_content_category ON content(category);
CREATE INDEX idx_content_status ON content(status);
CREATE INDEX idx_content_created_date ON content(created_date);
```

- [ ] **Step 2: Create V2 migration**

Create `backend/src/main/resources/db/migration/V2__create_tags_table.sql`:

```sql
CREATE TABLE tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE INDEX idx_tags_name ON tags(name);
```

- [ ] **Step 3: Create V3 migration**

Create `backend/src/main/resources/db/migration/V3__create_content_tags_table.sql`:

```sql
CREATE TABLE content_tags (
    content_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (content_id, tag_id),
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

CREATE INDEX idx_content_tags_tag_id ON content_tags(tag_id);
```

- [ ] **Step 4: Create V4 migration**

Create `backend/src/main/resources/db/migration/V4__create_fts_index.sql`:

```sql
CREATE VIRTUAL TABLE content_fts USING fts5(
    title,
    content_text,
    content='content',
    content_rowid='id'
);

CREATE TRIGGER content_ai AFTER INSERT ON content BEGIN
    INSERT INTO content_fts(rowid, title, content_text)
    VALUES (new.id, new.title, new.content_text);
END;

CREATE TRIGGER content_ad AFTER DELETE ON content BEGIN
    INSERT INTO content_fts(content_fts, rowid, title, content_text)
    VALUES ('delete', old.id, old.title, old.content_text);
END;

CREATE TRIGGER content_au AFTER UPDATE ON content BEGIN
    INSERT INTO content_fts(content_fts, rowid, title, content_text)
    VALUES ('delete', old.id, old.title, old.content_text);
    INSERT INTO content_fts(rowid, title, content_text)
    VALUES (new.id, new.title, new.content_text);
END;
```

- [ ] **Step 5: Verify app starts and migrations run**

Run: `cd backend && mvn spring-boot:run`
Expected: App starts on port 8080, Flyway runs 4 migrations, then stop the app.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/resources/db/
git commit -m "feat: add Flyway migrations for content, tags, content_tags, and FTS index"
```

---

### Task 3: JPA Entities

**Files:**
- Create: `backend/src/main/java/com/knowvault/content/Content.java`
- Create: `backend/src/main/java/com/knowvault/tag/Tag.java`

**Interfaces:**
- Produces: `Content` entity with fields: id (Long), title (String), contentText (String), url (String), source (String), category (String), author (String), createdDate (String), importDate (String), status (String), favorite (Boolean), tags (Set\<Tag\>)
- Produces: `Tag` entity with fields: id (Long), name (String), contents (Set\<Content\>)

- [ ] **Step 1: Create Content entity**

Create `backend/src/main/java/com/knowvault/content/Content.java`:

```java
package com.knowvault.content;

import com.knowvault.tag.Tag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "content")
@Getter @Setter @NoArgsConstructor
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    private String url;
    private String source;
    private String category;
    private String author;

    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "import_date")
    private String importDate;

    private String status;
    private Boolean favorite;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "content_tags",
        joinColumns = @JoinColumn(name = "content_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();
}
```

- [ ] **Step 2: Create Tag entity**

Create `backend/src/main/java/com/knowvault/tag/Tag.java`:

```java
package com.knowvault.tag;

import com.knowvault.content.Content;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Getter @Setter @NoArgsConstructor
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private Set<Content> contents = new HashSet<>();
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/knowvault/content/Content.java backend/src/main/java/com/knowvault/tag/Tag.java
git commit -m "feat: add Content and Tag JPA entities"
```

---

### Task 4: Repositories

**Files:**
- Create: `backend/src/main/java/com/knowvault/content/ContentRepository.java`
- Create: `backend/src/main/java/com/knowvault/tag/TagRepository.java`

**Interfaces:**
- Produces: `ContentRepository` with methods: findBySource, findByCategory, findByStatus, findByFavoriteTrue, findByUrlAndSource, searchByFullText
- Produces: `TagRepository` with methods: findByName, findByNames

- [ ] **Step 1: Create ContentRepository**

Create `backend/src/main/java/com/knowvault/content/ContentRepository.java`:

```java
package com.knowvault.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {

    List<Content> findBySource(String source);
    List<Content> findByCategory(String category);
    List<Content> findByStatus(String status);
    List<Content> findByFavoriteTrue();

    @Query("SELECT c FROM Content c WHERE c.url = :url AND c.source = :source")
    Optional<Content> findByUrlAndSource(@Param("url") String url, @Param("source") String source);

    @Query(value = "SELECT * FROM content WHERE id IN (SELECT rowid FROM content_fts WHERE content_fts MATCH :query)", nativeQuery = true)
    List<Content> searchByFullText(@Param("query") String query);
}
```

- [ ] **Step 2: Create TagRepository**

Create `backend/src/main/java/com/knowvault/tag/TagRepository.java`:

```java
package com.knowvault.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    @Query("SELECT t FROM Tag t WHERE t.name IN :names")
    List<Tag> findByNames(@Param("names") Collection<String> names);
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/knowvault/content/ContentRepository.java backend/src/main/java/com/knowvault/tag/TagRepository.java
git commit -m "feat: add ContentRepository and TagRepository"
```

---

### Task 5: DTOs and Exception Handling

**Files:**
- Create: `backend/src/main/java/com/knowvault/import_/dto/RawContent.java`
- Create: `backend/src/main/java/com/knowvault/import_/dto/ImportJob.java`
- Create: `backend/src/main/java/com/knowvault/import_/dto/ImportResult.java`
- Create: `backend/src/main/java/com/knowvault/import_/dto/ImportStatus.java`
- Create: `backend/src/main/java/com/knowvault/common/dto/ErrorResponse.java`
- Create: `backend/src/main/java/com/knowvault/common/exception/ImportException.java`
- Create: `backend/src/main/java/com/knowvault/common/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Produces: `RawContent` — fields: title, contentText, url, timestamp, author, sourceType (all String)
- Produces: `ImportJob` — fields: id, status, totalItems, processedItems, importedItems, skippedItems, errorMessage, startedAt, completedAt
- Produces: `ImportResult` — fields: imported (int), skipped (int), total (int)
- Produces: `ImportStatus` — fields: importId, status, progress, message
- Produces: `ErrorResponse` — fields: code, message
- Produces: `ImportException` — runtime exception for import errors

- [ ] **Step 1: Create RawContent DTO**

Create `backend/src/main/java/com/knowvault/import_/dto/RawContent.java`:

```java
package com.knowvault.import_.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RawContent {
    private String title;
    private String contentText;
    private String url;
    private String timestamp;
    private String author;
    private String sourceType;
}
```

- [ ] **Step 2: Create ImportJob DTO**

Create `backend/src/main/java/com/knowvault/import_/dto/ImportJob.java`:

```java
package com.knowvault.import_.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ImportJob {
    private String id;
    private String status;
    private int totalItems;
    private int processedItems;
    private int importedItems;
    private int skippedItems;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
```

- [ ] **Step 3: Create ImportResult DTO**

Create `backend/src/main/java/com/knowvault/import_/dto/ImportResult.java`:

```java
package com.knowvault.import_.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportResult {
    private int imported;
    private int skipped;
    private int total;
}
```

- [ ] **Step 4: Create ImportStatus DTO**

Create `backend/src/main/java/com/knowvault/import_/dto/ImportStatus.java`:

```java
package com.knowvault.import_.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImportStatus {
    private String importId;
    private String status;
    private int progress;
    private String message;
}
```

- [ ] **Step 5: Create ErrorResponse DTO**

Create `backend/src/main/java/com/knowvault/common/dto/ErrorResponse.java`:

```java
package com.knowvault.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
}
```

- [ ] **Step 6: Create ImportException**

Create `backend/src/main/java/com/knowvault/common/exception/ImportException.java`:

```java
package com.knowvault.common.exception;

public class ImportException extends RuntimeException {
    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 7: Create GlobalExceptionHandler**

Create `backend/src/main/java/com/knowvault/common/exception/GlobalExceptionHandler.java`:

```java
package com.knowvault.common.exception;

import com.knowvault.common.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ImportException.class)
    public ResponseEntity<ErrorResponse> handleImportException(ImportException e) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("IMPORT_ERROR", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
    }
}
```

- [ ] **Step 8: Verify compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/knowvault/import_/dto/ backend/src/main/java/com/knowvault/common/
git commit -m "feat: add DTOs and global exception handling"
```

---

### Task 6: FacebookArchiveParser

**Files:**
- Create: `backend/src/main/java/com/knowvault/import_/FacebookArchiveParser.java`

**Interfaces:**
- Consumes: `RawContent` DTO (from Task 5)
- Produces: `FacebookArchiveParser` with methods:
  - `validate(MultipartFile)` — throws ImportException if not a valid ZIP
  - `extract(MultipartFile)` → `Path` (temp directory)
  - `parsePosts(Path)` → `List<RawContent>`
  - `parseSavedItems(Path)` → `List<RawContent>`
  - `cleanup(Path)` — deletes temp directory

- [ ] **Step 1: Create test resources directory and sample archive**

Create test directory: `mkdir -p backend/src/test/resources/test-facebook-archive/posts backend/src/test/resources/test-facebook-archive/saved_items`

Create `backend/src/test/resources/test-facebook-archive/posts/your_posts_1.html`:

```html
<html><body>
<div class="post">
  <div class="title">Test Post Title</div>
  <div class="content">This is a test post content with some useful information.</div>
  <div class="timestamp">2024-01-15T10:30:00</div>
  <a href="https://facebook.com/user/posts/123">Original post</a>
</div>
</body></html>
```

Create `backend/src/test/resources/test-facebook-archive/saved_items/saved_items.json`:

```json
[
  {
    "title": "Saved Article About Java",
    "content": "This is a saved article about Java programming best practices.",
    "timestamp": "2024-02-20T14:00:00",
    "url": "https://example.com/java-article"
  },
  {
    "title": "Saved Recipe",
    "content": "A delicious recipe for chocolate cake.",
    "timestamp": "2024-03-10T09:00:00",
    "url": "https://example.com/recipe"
  }
]
```

- [ ] **Step 2: Create FacebookArchiveParser**

Create `backend/src/main/java/com/knowvault/import_/FacebookArchiveParser.java`:

```java
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
```

Note: This requires adding `jsoup` and `jackson-databind` dependencies to pom.xml. Add these to the `<dependencies>` section:

```xml
<dependency><groupId>org.jsoup</groupId><artifactId>jsoup</artifactId><version>1.17.2</version></dependency>
<dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId></dependency>
```

- [ ] **Step 3: Add jsoup and jackson dependencies to pom.xml**

Edit `backend/pom.xml` — add the two dependencies above to the `<dependencies>` section.

- [ ] **Step 4: Verify compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/knowvault/import_/FacebookArchiveParser.java backend/pom.xml backend/src/test/resources/
git commit -m "feat: add FacebookArchiveParser with HTML and JSON parsing"
```

---

### Task 7: ContentNormalizer

**Files:**
- Create: `backend/src/main/java/com/knowvault/import_/ContentNormalizer.java`

**Interfaces:**
- Consumes: `RawContent` (from Task 5), `Tag` entity (from Task 3)
- Produces: `ContentNormalizer` with methods:
  - `normalize(RawContent)` → `Content`
  - `extractTags(RawContent)` → `Set<Tag>`
  - `categorize(RawContent)` → `String`

- [ ] **Step 1: Create ContentNormalizer**

Create `backend/src/main/java/com/knowvault/import_/ContentNormalizer.java`:

```java
package com.knowvault.import_;

import com.knowvault.content.Content;
import com.knowvault.import_.dto.RawContent;
import com.knowvault.tag.Tag;
import org.springframework.stereotype.Component;

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
        content.setAuthor(raw.getAuthor() != null ? raw.getAuthor() : "");
        content.setCreatedDate(raw.getTimestamp());
        content.setStatus("unread");
        content.setFavorite(false);
        content.setCategory(categorize(raw));
        return content;
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
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/knowvault/import_/ContentNormalizer.java
git commit -m "feat: add ContentNormalizer with tag extraction and categorization"
```

---

### Task 8: ImportService

**Files:**
- Create: `backend/src/main/java/com/knowvault/import_/ImportService.java`

**Interfaces:**
- Consumes: `FacebookArchiveParser` (Task 6), `ContentNormalizer` (Task 7), `ContentRepository` (Task 4), `TagRepository` (Task 4), `RawContent` (Task 5), `ImportResult` (Task 5)
- Produces: `ImportService.importFacebookArchive(MultipartFile)` → `ImportResult`

- [ ] **Step 1: Create ImportService**

Create `backend/src/main/java/com/knowvault/import_/ImportService.java`:

```java
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

    @Transactional
    public ImportResult importFacebookArchive(MultipartFile file) {
        parser.validate(file);

        Path tempDir = null;
        int imported = 0;
        int skipped = 0;

        try {
            tempDir = parser.extract(file);

            List<RawContent> allRaw = new ArrayList<>();
            allRaw.addAll(parser.parsePosts(tempDir));
            allRaw.addAll(parser.parseSavedItems(tempDir));

            for (RawContent raw : allRaw) {
                try {
                    // Check for duplicates
                    if (raw.getUrl() != null && !raw.getUrl().isEmpty()) {
                        Optional<Content> existing = contentRepository.findByUrlAndSource(raw.getUrl(), "facebook");
                        if (existing.isPresent()) {
                            skipped++;
                            log.debug("Skipping duplicate: {}", raw.getUrl());
                            continue;
                        }
                    }

                    Content content = normalizer.normalize(raw);
                    Set<Tag> tags = normalizer.extractTags(raw);

                    // Resolve tags - reuse existing ones
                    Set<Tag> resolvedTags = resolveTags(tags);
                    content.setTags(resolvedTags);

                    contentRepository.save(content);
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
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/knowvault/import_/ImportService.java
git commit -m "feat: add ImportService with duplicate detection and tag resolution"
```

---

### Task 9: REST Controllers

**Files:**
- Create: `backend/src/main/java/com/knowvault/import_/ImportController.java`
- Create: `backend/src/main/java/com/knowvault/content/ContentController.java`
- Create: `backend/src/main/java/com/knowvault/tag/TagController.java`

**Interfaces:**
- Consumes: `ImportService` (Task 8), `ContentRepository` (Task 4), `TagRepository` (Task 4)
- Produces: REST endpoints:
  - `POST /api/import/facebook` — upload ZIP, returns ImportResult
  - `GET /api/content` — list all content
  - `GET /api/content/{id}` — get single content
  - `GET /api/content/search?q={query}` — full-text search
  - `PATCH /api/content/{id}/status` — update read status
  - `PATCH /api/content/{id}/favorite` — toggle favorite
  - `GET /api/tags` — list all tags

- [ ] **Step 1: Create ImportController**

Create `backend/src/main/java/com/knowvault/import_/ImportController.java`:

```java
package com.knowvault.import_;

import com.knowvault.import_.dto.ImportResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/facebook")
    public ResponseEntity<ImportResult> importFacebookArchive(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "deleteAfterImport", defaultValue = "true") boolean deleteAfterImport) {
        ImportResult result = importService.importFacebookArchive(file);
        return ResponseEntity.ok(result);
    }
}
```

- [ ] **Step 2: Create ContentController**

Create `backend/src/main/java/com/knowvault/content/ContentController.java`:

```java
package com.knowvault.content;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentRepository contentRepository;

    public ContentController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @GetMapping
    public ResponseEntity<List<Content>> listAll() {
        return ResponseEntity.ok(contentRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Content> getById(@PathVariable Long id) {
        return contentRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Content>> search(@RequestParam("q") String query) {
        return ResponseEntity.ok(contentRepository.searchByFullText(query));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Content> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return contentRepository.findById(id).map(content -> {
            content.setStatus(body.getOrDefault("status", content.getStatus()));
            return ResponseEntity.ok(contentRepository.save(content));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/favorite")
    public ResponseEntity<Content> toggleFavorite(@PathVariable Long id) {
        return contentRepository.findById(id).map(content -> {
            content.setFavorite(!Boolean.TRUE.equals(content.getFavorite()));
            return ResponseEntity.ok(contentRepository.save(content));
        }).orElse(ResponseEntity.notFound().build());
    }
}
```

- [ ] **Step 3: Create TagController**

Create `backend/src/main/java/com/knowvault/tag/TagController.java`:

```java
package com.knowvault.tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagRepository tagRepository;

    public TagController(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @GetMapping
    public ResponseEntity<List<Tag>> listAll() {
        return ResponseEntity.ok(tagRepository.findAll());
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/knowvault/import_/ImportController.java backend/src/main/java/com/knowvault/content/ContentController.java backend/src/main/java/com/knowvault/tag/TagController.java
git commit -m "feat: add REST controllers for import, content, and tags"
```

---

### Task 10: Tests

**Files:**
- Create: `backend/src/test/java/com/knowvault/import_/FacebookArchiveParserTest.java`
- Create: `backend/src/test/java/com/knowvault/import_/ContentNormalizerTest.java`
- Create: `backend/src/test/java/com/knowvault/import_/ImportServiceTest.java`
- Create: `backend/src/test/java/com/knowvault/content/ContentRepositoryTest.java`
- Create: `backend/src/test/java/com/knowvault/import_/ImportControllerTest.java`

**Interfaces:**
- Consumes: All components from Tasks 1-9
- Produces: Unit tests for parser and normalizer, integration tests for service and repository, controller tests for REST endpoints

- [ ] **Step 1: Create FacebookArchiveParserTest**

Create `backend/src/test/java/com/knowvault/import_/FacebookArchiveParserTest.java`:

```java
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
    void cleanup_removesDirectory(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("to-delete");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("file.txt"), "data");

        parser.cleanup(subDir);
        assertFalse(Files.exists(subDir));
    }
}
```

- [ ] **Step 2: Create ContentNormalizerTest**

Create `backend/src/test/java/com/knowvault/import_/ContentNormalizerTest.java`:

```java
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
```

- [ ] **Step 3: Create ImportServiceTest**

Create `backend/src/test/java/com/knowvault/import_/ImportServiceTest.java`:

```java
package com.knowvault.import_;

import com.knowvault.content.Content;
import com.knowvault.content.ContentRepository;
import com.knowvault.import_.dto.ImportResult;
import com.knowvault.import_.dto.RawContent;
import com.knowvault.tag.Tag;
import com.knowvault.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportServiceTest {

    private FacebookArchiveParser parser;
    private ContentNormalizer normalizer;
    private ContentRepository contentRepository;
    private TagRepository tagRepository;
    private ImportService importService;

    @BeforeEach
    void setUp() {
        parser = mock(FacebookArchiveParser.class);
        normalizer = new ContentNormalizer();
        contentRepository = mock(ContentRepository.class);
        tagRepository = mock(TagRepository.class);
        importService = new ImportService(parser, normalizer, contentRepository, tagRepository);
    }

    @Test
    void importFacebookArchive_importsNewContent() throws Exception {
        Path tempDir = Files.createTempDirectory("test-import");
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", new byte[0]);

        List<RawContent> rawPosts = List.of(
            RawContent.builder().title("Post 1").contentText("Content 1").url("https://fb.com/1").sourceType("post").build()
        );

        when(parser.extract(file)).thenReturn(tempDir);
        when(parser.parsePosts(tempDir)).thenReturn(rawPosts);
        when(parser.parseSavedItems(tempDir)).thenReturn(Collections.emptyList());
        when(contentRepository.findByUrlAndSource(any(), any())).thenReturn(Optional.empty());
        when(contentRepository.save(any(Content.class))).thenAnswer(i -> i.getArgument(0));
        when(tagRepository.findByName(any())).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(i -> i.getArgument(0));

        ImportResult result = importService.importFacebookArchive(file);
        assertEquals(1, result.getImported());
        assertEquals(0, result.getSkipped());
        verify(contentRepository).save(any(Content.class));
    }

    @Test
    void importFacebookArchive_skipsDuplicates() throws Exception {
        Path tempDir = Files.createTempDirectory("test-import");
        MockMultipartFile file = new MockMultipartFile("file", "test.zip", "application/zip", new byte[0]);

        List<RawContent> rawPosts = List.of(
            RawContent.builder().title("Post 1").contentText("Content 1").url("https://fb.com/1").sourceType("post").build()
        );

        when(parser.extract(file)).thenReturn(tempDir);
        when(parser.parsePosts(tempDir)).thenReturn(rawPosts);
        when(parser.parseSavedItems(tempDir)).thenReturn(Collections.emptyList());
        when(contentRepository.findByUrlAndSource("https://fb.com/1", "facebook"))
            .thenReturn(Optional.of(new Content()));

        ImportResult result = importService.importFacebookArchive(file);
        assertEquals(0, result.getImported());
        assertEquals(1, result.getSkipped());
        verify(contentRepository, never()).save(any());
    }
}
```

- [ ] **Step 4: Create ContentRepositoryTest**

Create `backend/src/test/java/com/knowvault/content/ContentRepositoryTest.java`:

```java
package com.knowvault.content;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ContentRepositoryTest {

    @Autowired
    private ContentRepository contentRepository;

    @Test
    void saveAndFindByUrlAndSource() {
        Content content = new Content();
        content.setTitle("Test");
        content.setContentText("Body");
        content.setUrl("https://example.com/1");
        content.setSource("facebook");
        content.setStatus("unread");
        content.setFavorite(false);
        contentRepository.save(content);

        Optional<Content> found = contentRepository.findByUrlAndSource("https://example.com/1", "facebook");
        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getTitle());
    }

    @Test
    void findByStatus_returnsMatchingContent() {
        Content c1 = new Content();
        c1.setTitle("Unread");
        c1.setSource("facebook");
        c1.setStatus("unread");
        c1.setFavorite(false);
        contentRepository.save(c1);

        Content c2 = new Content();
        c2.setTitle("Read");
        c2.setSource("facebook");
        c2.setStatus("completed");
        c2.setFavorite(false);
        contentRepository.save(c2);

        assertEquals(1, contentRepository.findByStatus("unread").size());
    }

    @Test
    void uniqueConstraint_preventsDuplicateUrlSource() {
        Content c1 = new Content();
        c1.setTitle("First");
        c1.setSource("facebook");
        c1.setUrl("https://example.com/dup");
        c1.setStatus("unread");
        c1.setFavorite(false);
        contentRepository.save(c1);

        Content c2 = new Content();
        c2.setTitle("Second");
        c2.setSource("facebook");
        c2.setUrl("https://example.com/dup");
        c2.setStatus("unread");
        c2.setFavorite(false);

        assertThrows(Exception.class, () -> {
            contentRepository.save(c2);
            contentRepository.flush();
        });
    }
}
```

- [ ] **Step 5: Create ImportControllerTest**

Create `backend/src/test/java/com/knowvault/import_/ImportControllerTest.java`:

```java
package com.knowvault.import_;

import com.knowvault.import_.dto.ImportResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockbean.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImportController.class)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportService importService;

    @Test
    void importFacebook_returnsResult() throws Exception {
        ImportResult result = new ImportResult(5, 1, 6);
        when(importService.importFacebookArchive(any())).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile("file", "archive.zip", "application/zip", "data".getBytes());

        mockMvc.perform(multipart("/api/import/facebook").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.imported").value(5))
            .andExpect(jsonPath("$.skipped").value(1))
            .andExpect(jsonPath("$.total").value(6));
    }
}
```

- [ ] **Step 6: Run all tests**

Run: `cd backend && mvn test`
Expected: All tests pass (BUILD SUCCESS)

- [ ] **Step 7: Commit**

```bash
git add backend/src/test/
git commit -m "feat: add unit, integration, and controller tests"
```

---

## Plan Summary

| Task | Component | Depends On |
|:---|:---|:---|
| 1 | Scaffold Spring Boot Project | — |
| 2 | Database Migrations | 1 |
| 3 | JPA Entities | 1 |
| 4 | Repositories | 3 |
| 5 | DTOs and Exception Handling | — |
| 6 | FacebookArchiveParser | 5 |
| 7 | ContentNormalizer | 5 |
| 8 | ImportService | 4, 6, 7 |
| 9 | REST Controllers | 4, 8 |
| 10 | Tests | All above |
