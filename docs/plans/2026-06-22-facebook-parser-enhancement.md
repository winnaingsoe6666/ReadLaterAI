# KnowVault Facebook Archive Parser Enhancement Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development

**Goal:** Expand Facebook archive parsing from 2 data types to 10+ — adding Messenger, Groups, Comments, and interest metadata.

**Architecture:** Extend existing `FacebookArchiveParser` with new parse methods. Add new `sourceType` values. Update `ContentNormalizer` and `ImportService`. No new dependencies.

**Tech Stack:** Java 21, Spring Boot 3.3, Jsoup, Jackson, SQLite

## Global Constraints

- No new Maven dependencies
- Try JSON first, fall back to HTML (existing pattern)
- New sourceTypes: `messenger_message`, `group_post`, `group_commented_post`, `comment`, `liked_page`, `ad_preference`
- Error handling: log and skip individual items, don't abort import
- Support both `your_facebook_activity/` and top-level dirs

## Dependency Graph

```
Task 1 (Constants) ─┬→ Task 2 (Messenger) ──┐
                     ├→ Task 3 (Groups) ─────┤
                     └→ Task 4 (Comments) ───┼→ Task 6 (Normalizer) → Task 7 (ImportService)
                          Task 5 (Metadata) ─┘
```

---

### Task 1: Add SourceType Constants to RawContent

**Files:** Modify `backend/src/main/java/com/knowvault/import_/dto/RawContent.java`
**Depends on:** None

- [ ] **Step 1:** Add static constants:
  ```java
  public static final String TYPE_POST = "post";
  public static final String TYPE_SAVED_ITEM = "saved_item";
  public static final String TYPE_MESSENGER_MESSAGE = "messenger_message";
  public static final String TYPE_GROUP_POST = "group_post";
  public static final String TYPE_GROUP_COMMENTED_POST = "group_commented_post";
  public static final String TYPE_COMMENT = "comment";
  public static final String TYPE_LIKED_PAGE = "liked_page";
  public static final String TYPE_AD_PREFERENCE = "ad_preference";
  ```

- [ ] **Step 2:** Add `metadata` field (`Map<String, String>`) to RawContent for interest metadata types. Default to empty map in builder.

- [ ] **Step 3:** Ensure existing code compiles with new field (metadata defaults to empty map).
### Task 2: Messenger Messages Parser

**Files:** Modify `backend/src/main/java/com/knowvault/import_/FacebookArchiveParser.java`
**Depends on:** Task 1

- [ ] **Step 1:** Add `parseMessages(Path archiveDir)` method:
  - Check `your_messenger_activity/messages/inbox/` (new format)
  - Fall back to `messages/inbox/` (old format)
  - List all conversation directories under `inbox/`
  - For each conversation, find all `message_*.json` files
  - Return aggregated `List<RawContent>`

- [ ] **Step 2:** Add `parseConversationMessages(Path dir, String title)` helper:
  - Read `message_1.json`, `message_2.json`, etc.
  - Parse JSON as `Map<String, Object>` (same pattern as `parseJsonPosts`)
  - Extract `messages` array
  - For each message with `share.link` non-empty, build RawContent:
    - `title`: "{sender_name} shared in {conversation_title}"
    - `contentText`: `content` or `share.share_text`
    - `url`: `share.link`
    - `timestamp`: convert `timestamp_ms` (millis)
    - `author`: `sender_name`
    - `sourceType`: `TYPE_MESSENGER_MESSAGE`

- [ ] **Step 3:** Edge cases:
  - Skip `is_unsent: true` messages
  - Skip messages without `share` (unless `content` > 100 chars)
  - Process all `message_*.json` files per conversation
  - Deduplicate by URL across conversations

- [ ] **Step 4:** Add HTML fallback for old format `messages.htm`:
  - Parse with Jsoup, extract links from message divs
  - Same RawContent structure
### Task 3: Groups Parser

**Files:** Modify `backend/src/main/java/com/knowvault/import_/FacebookArchiveParser.java`
**Depends on:** Task 1

- [ ] **Step 1:** Add `parseGroupPosts(Path archiveDir)`:
  - Check `your_facebook_activity/groups/your_posts_in_groups/` (new)
  - Fall back to `groups/your_posts_in_groups/` (old)
  - Find `your_posts_in_groups_*.json` files
  - Parse using same logic as `parseJsonPosts` (identical JSON structure)
  - Set `sourceType` = `TYPE_GROUP_POST`

- [ ] **Step 2:** Add `parseGroupCommentedPosts(Path archiveDir)`:
  - Check `your_facebook_activity/groups/posts_commented_on_in_groups/` (new)
  - Fall back to `groups/posts_commented_on_in_groups/` (old)
  - Parse JSON — similar to posts but includes `comments` array
  - Extract post content and URLs
  - Set `sourceType` = `TYPE_GROUP_COMMENTED_POST`

- [ ] **Step 3:** Add `parseGroupComments(Path archiveDir)`:
  - Check `your_facebook_activity/groups/your_comments_in_groups/` (new)
  - Fall back to `groups/your_comments_in_groups/` (old)
  - Parse comment text and attached URLs
  - Set `sourceType` = `TYPE_COMMENT`

- [ ] **Step 4:** All group parsers handle:
  - Missing `attachments` gracefully
  - Empty `data` arrays
  - Both `your_facebook_activity/groups/` and top-level `groups/` paths
  - Log count per file (existing pattern)
### Task 4: Comments Parser

**Files:** Modify `backend/src/main/java/com/knowvault/import_/FacebookArchiveParser.java`
**Depends on:** Task 1

- [ ] **Step 1:** Add `parseComments(Path archiveDir)`:
  - Check `your_facebook_activity/comments/` (new format)
  - Fall back to `comments/` (old format)
  - Find `comments_*.json` files
  - Parse JSON: array with `timestamp` and `data[]` containing `comment` objects
  - Each comment has: `comment.comment` (text), `comment.author`, `comment.title`, `comment.attachment.uri`
  - Build RawContent:
    - `title`: comment.title (e.g., "Win Naing Soe commented on Tech Blog's post.")
    - `contentText`: comment.comment (your actual comment)
    - `url`: comment.attachment.uri if present
    - `timestamp`: from outer timestamp field
    - `author`: comment.author
    - `sourceType`: TYPE_COMMENT

- [ ] **Step 2:** Edge cases:
  - Comments without attachment — url = empty
  - Short comments ("nice!") — still include
  - Multiple files (comments_1.json, comments_2.json) — process all
  - Nested data array — iterate correctly

- [ ] **Step 3:** HTML fallback for `comments/*.html`:
  - Parse with Jsoup similar to parseNewHtmlPosts
  - Extract comment text, author, timestamp
### Task 5: Interest Metadata Parser

**Files:** Modify `backend/src/main/java/com/knowvault/import_/FacebookArchiveParser.java`
**Depends on:** Task 1

- [ ] **Step 1:** Add `parseLikedPages(Path archiveDir)`:
  - Check `your_facebook_activity/likes_and_reactions/pages.json`
  - Fall back to `likes_and_reactions/pages.json` or `pages/`
  - Parse JSON: `pages_v2` array with `name` and `timestamp`
  - Build RawContent: title=page name, sourceType=TYPE_LIKED_PAGE, metadata={"page_name":"<name>"}

- [ ] **Step 2:** Add `parseAdPreferences(Path archiveDir)`:
  - Check `your_facebook_activity/ads_and_businesses/your_ad_preferences.json`
  - Fall back to `ads_information/your_ad_preferences.json`
  - Parse JSON: `topics` and `interests` arrays
  - Build RawContent per interest: title=name, sourceType=TYPE_AD_PREFERENCE

- [ ] **Step 3:** Add `parseSearchHistory(Path archiveDir)`:
  - Check `your_facebook_activity/search_history/your_search_history.json`
  - Parse JSON: `searches` array with `timestamp` and `data[].text`
  - Build RawContent: title=query text, sourceType="search_query"

- [ ] **Step 4:** Metadata types stored separately from content. ImportService will check sourceType to distinguish content vs metadata.
### Task 6: Update ContentNormalizer

**Files:** Modify `backend/src/main/java/com/knowvault/import_/ContentNormalizer.java`
**Depends on:** Tasks 2, 3, 4, 5

- [ ] **Step 1:** Handle new sourceTypes in normalize():
  - `messenger_message`: author=sender_name directly, category="Messenger", tags=["messenger","shared-link"]
  - `group_post`: extract group name from title, category="Groups", tags=["group","{group_name}"]
  - `group_commented_post`: extract group name, category="Groups", tags=["group","commented"]
  - `comment`: use title as-is, category="Comments", tags=["comment"]

- [ ] **Step 2:** Metadata types (liked_page, ad_preference, search_query) should NOT create Content items. Add `isMetadata(sourceType)` check.

- [ ] **Step 3:** Update extractCategory() for new types: messenger→"Messenger", group→"Groups", comment→"Comments"

- [ ] **Step 4:** Update extractTags() for new types: messenger gets domain from URL, groups get group name, comments get "comment" tag

---

### Task 7: Update ImportService Orchestration

**Files:** Modify `backend/src/main/java/com/knowvault/import_/ImportService.java`
**Depends on:** Task 6

- [ ] **Step 1:** Call new parsers in importFacebookArchive():
  ```java
  List<RawContent> messenger = parser.parseMessages(archiveDir);
  List<RawContent> groupPosts = parser.parseGroupPosts(archiveDir);
  List<RawContent> groupCommented = parser.parseGroupCommentedPosts(archiveDir);
  List<RawContent> comments = parser.parseComments(archiveDir);
  List<RawContent> likedPages = parser.parseLikedPages(archiveDir);
  List<RawContent> adPrefs = parser.parseAdPreferences(archiveDir);
  ```

- [ ] **Step 2:** Combine all results:
  ```java
  List<RawContent> allContent = new ArrayList<>();
  allContent.addAll(posts);
  allContent.addAll(savedItems);
  allContent.addAll(messenger);
  allContent.addAll(groupPosts);
  allContent.addAll(groupCommented);
  allContent.addAll(comments);
  ```

- [ ] **Step 3:** Handle metadata separately:
  - Filter out metadata types (liked_page, ad_preference) from content processing
  - Store metadata for AI context or skip entirely for MVP
  - Log counts: "X posts, Y messenger links, Z group posts, W comments, M metadata items"

- [ ] **Step 4:** Update deduplication to work across all content types (dedupe by URL)

---

### Task 8: Test Data

**Files:** Create test fixtures in `backend/src/test/resources/test-facebook-archive/`
**Depends on:** None (can parallel with implementation)

- [ ] **Step 1:** Create `messages/inbox/TestFriend/message_1.json` with sample Messenger data including shared links

- [ ] **Step 2:** Create `groups/your_posts_in_groups/your_posts_in_groups_1.json` with sample group post

- [ ] **Step 3:** Create `groups/posts_commented_on_in_groups/posts_commented_on_in_groups_1.json`

- [ ] **Step 4:** Create `comments/comments_1.json` with sample comment data

- [ ] **Step 5:** Create `likes_and_reactions/pages.json` with sample liked pages

---

## Summary Table

| Task | Description | Files | Steps | Depends On |
|------|-------------|-------|-------|------------|
| 1 | SourceType Constants | 1 modify | 3 | — |
| 2 | Messenger Parser | 1 modify | 4 | 1 |
| 3 | Groups Parser | 1 modify | 4 | 1 |
| 4 | Comments Parser | 1 modify | 3 | 1 |
| 5 | Metadata Parser | 1 modify | 4 | 1 |
| 6 | ContentNormalizer | 1 modify | 4 | 2,3,4,5 |
| 7 | ImportService | 1 modify | 4 | 6 |
| 8 | Test Data | 5 create | 5 | — |

**Totals:** 8 tasks · 5 files created · 5 files modified · 31 steps
