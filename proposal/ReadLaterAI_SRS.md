# ReadLater AI / Knowledge Inbox
## Software Requirements Specification (SRS)

**Version:** 1.0  
**Author:** Win Naing Soe  
**Date:** June 2026  

---

### 1. Project Overview

#### 1.1 Problem Statement
Many users save useful content from Facebook, LinkedIn, YouTube, Reddit, Telegram, blogs, and other social media platforms. 

The common behavior is:
1. User sees useful content.
2. User shares the post with "Only Me" visibility or saves it.
3. User intends to read it later.
4. Saved posts accumulate over months or years.
5. User can no longer find, organize, or revisit valuable information.

As a result, users lose access to their own collected knowledge. The goal of this project is to transform saved content into a searchable personal knowledge base.

---

### 2. Product Vision
ReadLater AI is a privacy-first personal knowledge management system. The application helps users:
* Import previously saved content
* Organize information automatically
* Search saved knowledge efficiently
* Generate AI summaries
* Build a personal second brain

All user data remains under the user's control.

---

### 3. Target Users

#### Primary Users
Knowledge workers, including:
* Software Engineers
* Students
* Researchers
* Content Creators
* Product Managers
* Entrepreneurs

#### Secondary Users
General social media users who frequently save content but rarely revisit it. Examples include:
* Facebook users
* LinkedIn users
* YouTube users
* Reddit users

---

### 4. Product Goals
The system should achieve the following:
* Import existing saved content.
* Prevent future knowledge loss.
* Create a searchable knowledge base.
* Provide AI-powered summaries.
* Preserve user privacy.
* Operate with minimal or zero recurring cost.

---

### 5. Scope

#### In Scope (MVP)
* Facebook archive import
* Local storage
* Search (Keyword & Advanced)
* Tagging & Categories
* AI summaries & Knowledge retrieval (RAG)
* Export functionality

#### Out of Scope (MVP)
* Real-time Facebook integration
* Facebook login / API integration
* Multi-user cloud synchronization
* Social sharing features

---

### 6. Functional Requirements

#### FR-001: Import Facebook Archive
* **Description:** User shall import a Facebook archive ZIP file.
* **Inputs:** ZIP file
* **System Actions:** * Validate archive structure
  * Extract data
  * Parse posts, saved items, and shared items
* **Output:** Imported content records

#### FR-002: Archive Storage Options
* **Description:** User shall choose between retaining or cleaning up files.
  * **Option A:** Keep original archive
  * **Option B:** Delete archive after import
* **Default:** Option B (Delete archive after import)

#### FR-003: Content Extraction
* **Description:** System shall extract the following metadata and content fields:
  * Title
  * URL
  * Content text
  * Post date
  * Source platform
  * Author (if available)

#### FR-004: Automatic Categorization
* **Description:** System shall automatically categorize content.
* **Target Categories:** Programming, Business, Finance, Health, AI, Productivity, Marketing, etc.

#### FR-005: Tag Generation
* **Description:** System shall generate tags based on content.
* **Example:** * *Content:* "Spring Boot Redis Cache"
  * *Tags:* `Spring Boot`, `Redis`, `Backend`, `Java`

#### FR-006: Search
* **Description:** User shall search content using keyword queries.
* **Examples:** `redis`, `aws`, `rabbitmq`, `system design`

#### FR-007: Advanced Search
* **Description:** Support filtering search results by:
  * Category
  * Tags
  * Date Range
  * Source Platform

#### FR-008: Bookmark Status
* **Description:** User can mark items with status flags: `Unread`, `Reading`, `Completed`.

#### FR-009: Favorites
* **Description:** User can favorite/star important items.

#### FR-010: AI Summary
* **Description:** System shall generate summaries at various lengths: `Short Summary`, `Medium Summary`, `Detailed Summary`.

#### FR-011: AI Question Answering
* **Description:** User can query their data naturally.
* **Examples:**
  * *"What Redis articles have I saved?"*
  * *"Show me all AWS architecture content."*
  * *"What productivity advice did I save last year?"*
* **System Action:** Returns relevant contextually filtered results.

#### FR-012: Export
* **Description:** Export formats supported: `Markdown`, `PDF`, `JSON`.

---

### 7. AI Requirements

#### AI-001: AI Provider
Supported providers:
* Google Gemini API
* Local Ollama
* No AI Mode

#### AI-002: User API Key
* **Description:** System shall allow users to enter their Google AI Studio API Key.
* **Security:** System must never store the API key remotely.

#### AI-003: Summary Generation Output
The structured AI generation must output:
* Title
* Summary
* Key Takeaways
* Tags

#### AI-004: RAG Search
* **Description:** System shall support Retrieval-Augmented Generation.
* **Flow:** `Search Query` &rarr; `Vector Search` &rarr; `Relevant Documents` &rarr; `AI Response`

---

### 8. Non-Functional Requirements

#### Performance
* **Archive Import:** Up to 1,000 posts processed in less than 2 minutes.
* **Search Response:** Less than 1 second.
* **AI Response:** Less than 10 seconds.

---

### 9. Security Requirements

* **Local First:** All imported data remains strictly on the local device.
* **Data Privacy:** No automatic uploads, no telemetry, and no analytics.
* **Encryption:** Optional local database encryption.

---

### 10. Desktop Application Requirements

#### Platform Support
* **Supported OS:** Windows 10, Windows 11 (macOS and Linux planned for future releases).

#### Installer
* One-click installation.
* The installer will: Verify environment, create the local database, and configure the application.

---

### 11. Database Design

#### Table: `content`
| Field | Type / Description |
| :--- | :--- |
| `id` | Primary Key |
| `title` | Text |
| `content` | Text |
| `url` | Text |
| `source` | Text |
| `category` | Text |
| `created_date` | Date |
| `import_date` | Date |
| `status` | Text |
| `favorite` | Boolean |

#### Table: `tags`
* `id` (Primary Key)
* `name`

#### Table: `content_tags`
* `content_id` (Foreign Key)
* `tag_id` (Foreign Key)

#### Table: `summaries`
* `content_id` (Foreign Key)
* `summary`
* `key_points`
* `generated_at`

---

### 12. Future Features

* **Phase 2:** Chrome Extension (Save content directly from the browser).
* **Phase 3:** Android App (Share directly to ReadLater AI).
* **Phase 4:** Multi-device synchronization.
* **Phase 5:** Cloud Backup.

---

### 13. Technical Stack

* **Backend:** Java 21, Spring Boot 3
* **Frontend:** React, Electron
* **Database:** SQLite
* **Search:** SQLite FTS, Qdrant (optional)
* **AI:** Gemini API, Ollama
* **Deployment:** Standalone Desktop Application

---

### 14. Success Criteria

A successful MVP shall allow a user to:
1. Import Facebook archive smoothly.
2. Search and discover saved knowledge instantly.
3. Generate comprehensive AI summaries.
4. Ask contextual questions about their saved content.
5. Use the system autonomously without monthly recurring fees.
6. Maintain full control over personal data privacy.

***

*End of Document*
