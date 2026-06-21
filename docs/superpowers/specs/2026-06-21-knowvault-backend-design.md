# KnowVault Backend Design: SQLite Schema & Import Module

**Date:** 2026-06-21  
**Author:** Win Naing Soe  
**Status:** Approved  
**Scope:** MVP Backend — SQLite database schema and Facebook archive import

---

## 1. Overview

This document describes the backend architecture for KnowVault, focusing on:
- SQLite database schema (content, tags, content_tags tables)
- Import Module for Facebook ZIP archive extraction
- Project structure using Java 21, Spring Boot 3, and Flyway

**Tech Stack:**
- Java 21
- Spring Boot 3.3.0
- SQLite (via sqlite-jdbc + Hibernate community dialect)
- Flyway for schema migrations
- Spring Data JPA for data access

---

## 2. Project Structure

### Architecture Approach: Feature-Based Modular Monolith

```
com.knowvault/
├── KnowVaultApplication.java
├── common/
│   ├── config/           ← SQLite, JPA, CORS configuration
│   ├── exception/        ← Global error handling
│   └── dto/              ← Shared DTOs (PageResponse, etc.)
├── content/
│   ├── Content.java              ← JPA Entity
│   ├── ContentRepository.java    ← Spring Data JPA
│   ├── ContentService.java
│   └── ContentController.java
├── tag/
│   ├── Tag.java
│   ├── TagRepository.java
│   ├── TagService.java
│   └── TagController.java
└── import_/
    ├── ImportController.java
    ├── ImportService.java
    ├── FacebookArchiveParser.java
    └── ContentNormalizer.java
```

### Directory Layout

```
knowvault-backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/knowvault/
│   │   │   └── (packages above)
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__create_content_table.sql
│   │           ├── V2__create_tags_table.sql
│   │           ├── V3__create_content_tags_table.sql
│   │           └── V4__create_fts_index.sql
│   └── test/
│       └── java/com/knowvault/
└── data/                           ← SQLite DB file location
```

### Key Dependencies

| Dependency | Purpose |
|:---|:---|
| spring-boot-starter-web | REST API |
| spring-boot-starter-data-jpa | Data access |
| sqlite-jdbc (3.45.3.0) | SQLite driver |
| hibernate-community-dialects | SQLite dialect for Hibernate |
| flyway-core | Schema migrations |
| spring-boot-starter-validation | Input validation |
| lombok (optional) | Reduce boilerplate |

### application.yml

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

server:
  port: 8080

knowvault:
  import:
    temp-dir: ./data/temp
    max-file-size: 500MB
```

---

## 3. Database Schema

### Table: content

| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique identifier |
| title | TEXT | | Content title |
| content_text | TEXT | | Full content body |
| url | TEXT | | Source URL |
| source | TEXT | NOT NULL DEFAULT 'facebook' | Platform source |
| category | TEXT | | Auto-categorized topic |
| author | TEXT | | Content author |
| created_date | TEXT | | Original post date (ISO 8601) |
| import_date | TEXT | NOT NULL DEFAULT datetime('now') | Import timestamp |
| status | TEXT | NOT NULL DEFAULT 'unread' | unread, reading, completed |
| favorite | INTEGER | NOT NULL DEFAULT 0 | Boolean (0/1) |

**Constraints:**
- UNIQUE(url, source) — prevents duplicate imports

**Indexes:**
- idx_content_source
- idx_content_category
- idx_content_status
- idx_content_created_date

### Table: tags

| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| id | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique identifier |
| name | TEXT | NOT NULL UNIQUE | Tag name |

**Indexes:**
- idx_tags_name

### Table: content_tags

| Column | Type | Constraints | Description |
|:---|:---|:---|:---|
| content_id | INTEGER | NOT NULL, FK → content(id) ON DELETE CASCADE | Content reference |
| tag_id | INTEGER | NOT NULL, FK → tags(id) ON DELETE CASCADE | Tag reference |

**Constraints:**
- PRIMARY KEY (content_id, tag_id)

**Indexes:**
- idx_content_tags_tag_id

### Table: content_fts (Virtual Table)

SQLite FTS5 virtual table for full-text search:

```sql
CREATE VIRTUAL TABLE content_fts USING fts5(
    title,
    content_text,
    content='content',
    content_rowid='id'
);
```

**Triggers:** Keep FTS index synchronized with content table changes (INSERT, UPDATE, DELETE).

---

## 4. JPA Entities

### Content.java

```java
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

### Tag.java

```java
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

---

## 5. Repositories

### ContentRepository

```java
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

### TagRepository

```java
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    Optional<Tag> findByName(String name);
    
    @Query("SELECT t FROM Tag t WHERE t.name IN :names")
    List<Tag> findByNames(@Param("names") Collection<String> names);
}
```

---

## 6. Import Module

### Facebook Archive Structure (Typical)

```
facebook-<username>/
├── posts/
│   ├── your_posts_1.html (or .json)
│   └── ...
├── saved_items/
│   └── saved_items.json (or HTML)
├── comments/
├── likes_and_reactions/
└── ...
```

**MVP Focus:** `posts/` and `saved_items/` directories.

### Import Flow

```
POST /api/import/facebook
         │
         ▼
┌─────────────────────────────────────────────────┐
│                ImportService                     │
│  1. Validate ZIP structure                       │
│  2. Extract to temp directory                    │
│  3. Parse posts/ and saved_items/                │
│  4. Normalize RawContent → Content               │
│  5. Extract tags                                 │
│  6. Save to database (skip duplicates)           │
│  7. Cleanup temp files                           │
└─────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────┐
│  FacebookArchiveParser                           │
│  - validate(MultipartFile)                       │
│  - extract(MultipartFile) → Path                 │
│  - parsePosts(Path) → List<RawContent>           │
│  - parseSavedItems(Path) → List<RawContent>      │
│  - cleanup(Path)                                 │
└─────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────┐
│  ContentNormalizer                               │
│  - normalize(RawContent) → Content               │
│  - extractTags(RawContent) → Set<Tag>            │
│  - categorize(RawContent) → String               │
└─────────────────────────────────────────────────┘
```

### Component Details

#### ImportController

```java
@RestController
@RequestMapping("/api/import")
public class ImportController {
    
    @PostMapping("/facebook")
    public ResponseEntity<ImportResult> importFacebookArchive(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "deleteAfterImport", defaultValue = "true") boolean deleteAfterImport);
    
    @GetMapping("/status/{importId}")
    public ResponseEntity<ImportStatus> getImportStatus(@PathVariable String importId);
}
```

#### ImportService

Orchestrates the import process:
1. Validates the uploaded ZIP file
2. Extracts to temporary directory
3. Delegates parsing to FacebookArchiveParser
4. Normalizes data via ContentNormalizer
5. Persists to database, skipping duplicates
6. Cleans up temporary files

**Transaction:** Entire import is wrapped in @Transactional.

#### FacebookArchiveParser

Handles ZIP extraction and parsing of Facebook-specific file formats (HTML or JSON).

**Key methods:**
- `validate(MultipartFile)` — checks file is non-empty ZIP
- `extract(MultipartFile)` — extracts to temp directory
- `parsePosts(Path)` — parses posts/ directory
- `parseSavedItems(Path)` — parses saved_items/ directory
- `cleanup(Path)` — deletes temp directory

#### ContentNormalizer

Transforms raw parsed data into database entities:

- `normalize(RawContent)` → Content entity
- `extractTags(RawContent)` → Set of Tag entities (keyword extraction)
- `categorize(RawContent)` → Category string (keyword matching)

### DTOs

#### ImportJob (import tracking)

```java
@Data @Builder
public class ImportJob {
    private String id;
    private String status;      // PENDING, IN_PROGRESS, COMPLETED, FAILED
    private int totalItems;
    private int processedItems;
    private int importedItems;
    private int skippedItems;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
```

#### ImportStatus

```java
@Data @AllArgsConstructor
public class ImportStatus {
    private String importId;
    private String status;
    private int progress;       // 0-100 percentage
    private String message;
}
```

#### ErrorResponse

```java
@Data @AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
}
```

#### RawContent (intermediate representation)

```java
@Data @Builder
public class RawContent {
    private String title;
    private String contentText;
    private String url;
    private String timestamp;
    private String author;
    private String sourceType;  // "post" or "saved_item"
}
```

#### ImportResult

```java
@Data @AllArgsConstructor
public class ImportResult {
    private int imported;
    private int skipped;
    private int total;
}
```

---

## 7. Testing Strategy

### Unit Tests

- **ContentNormalizerTest** — normalize(), extractTags(), categorize()
- **FacebookArchiveParserTest** — validate(), parsePosts(), parseSavedItems()

### Integration Tests

- **ImportServiceTest** — full import flow with test ZIP archive
- **ContentRepositoryTest** — FTS search, duplicate detection

### Controller Tests

- **ImportControllerTest** — MockMvc tests for REST endpoints

### Test Resources

```
src/test/resources/
├── sample-facebook.zip
└── test-facebook-archive/
    ├── posts/
    │   └── your_posts_1.html
    └── saved_items/
        └── saved_items.json
```

---

## 8. Performance Targets

| Metric | Target |
|:---|:---|
| Archive Import (1000 posts) | < 2 minutes |
| Search Response | < 1 second |
| Memory Usage | < 512MB during import |

---

## 9. Error Handling

### Exceptions

- `ImportException` — ZIP validation failure, extraction errors
- `DuplicateContentException` — URL already imported (logged, not thrown)
- `ParseException` — malformed HTML/JSON in archive

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ImportException.class)
    public ResponseEntity<ErrorResponse> handleImportException(ImportException e) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("IMPORT_ERROR", e.getMessage()));
    }
}
```

---

## 10. Next Steps

1. Scaffold Spring Boot project with Maven
2. Implement database migrations (V1-V4)
3. Create JPA entities and repositories
4. Implement FacebookArchiveParser
5. Implement ContentNormalizer
6. Implement ImportService
7. Create REST endpoints
8. Write tests
9. Create sample Facebook archive for testing

---

## Appendix A: Flyway Migrations

### V1__create_content_table.sql

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

### V2__create_tags_table.sql

```sql
CREATE TABLE tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE INDEX idx_tags_name ON tags(name);
```

### V3__create_content_tags_table.sql

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

### V4__create_fts_index.sql

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

---

*End of Design Document*
