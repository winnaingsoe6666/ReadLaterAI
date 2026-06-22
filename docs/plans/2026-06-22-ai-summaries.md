# KnowVault AI Summaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Add AI-powered content summarization — backend AI provider abstraction (Gemini/Ollama/NoOp), summary CRUD API, and frontend UI integrated into ContentDetail page.

**Architecture:** Spring Boot backend with Strategy pattern for AI providers (Gemini REST API, Ollama local API, NoOp fallback). Summaries stored in SQLite `summaries` table. React frontend with custom hook, service layer, and SummarySection component embedded in ContentDetail.

**Tech Stack:** Java 21, Spring Boot 3.3, SQLite, Flyway, WebClient/RestTemplate (no new Maven deps), React 19, TypeScript 6, Tailwind CSS 4, Lucide React

## Global Constraints

- No new npm dependencies beyond what's already installed
- No new Maven dependencies — use existing Spring WebClient (included via spring-boot-starter-web) and RestTemplate for HTTP calls
- All AI calls enforce a 10-second timeout (SRS NFR)
- Default `provider: none` ensures app works without any AI setup (SRS AI-001)
- API key is local-only, stored in application.yml, never transmitted to KnowVault server (SRS AI-002)
- TypeScript strict mode; follow existing codebase patterns (mountedRef hooks, axios services, CSS custom properties)
- Summary key_points stored as JSON array string in SQLite TEXT column

## Directory Structure

```
backend/src/main/java/com/knowvault/
├── ai/
│   ├── AIProvider.java          (interface)
│   ├── GeminiProvider.java
│   ├── OllamaProvider.java
│   ├── NoOpProvider.java
│   └── AIConfig.java            (@Configuration)
└── summary/
    ├── Summary.java             (JPA entity)
    ├── SummaryRepository.java
    ├── SummaryService.java
    ├── SummaryController.java
    └── dto/
        ├── AIResponse.java
        └── SummaryResponse.java

backend/src/main/resources/db/migration/
└── V5__create_summaries_table.sql

frontend/src/
├── types/
│   ├── summary.ts               (new)
│   └── index.ts                 (modify)
├── services/
│   ├── summaryService.ts        (new)
│   └── index.ts                 (modify)
├── hooks/
│   ├── useSummary.ts            (new)
│   └── index.ts                 (modify)
├── components/
│   ├── ui/
│   │   └── SummaryCard.tsx      (new)
│   └── summary/
│       └── SummarySection.tsx   (new)
└── pages/
    └── ContentDetail.tsx        (modify)
```

## Dependency Graph

```
Task 1 (DB Migration) ─┐
                        ├─→ Task 2 (Entity + Repo) ─┐
Task 3 (AI Config) ─────┤                            ├─→ Task 5 (Summary Service) ─→ Task 6 (Controller)
                        └─→ Task 4 (AI Providers) ───┘

Task 7 (Types) ─┐
                ├─→ Task 8 (Service) ─→ Task 9 (Hook) ─→ Task 10 (SummaryCard)
Task 7 (Types) ─┘                                        └─→ Task 11 (SummarySection) ─→ Task 12 (Integrate)

Backend: Tasks 1-6
Frontend: Tasks 7-12
Backend & Frontend tracks can run in parallel until Task 12 (needs backend running)
```

---

### Task 1: Database Migration — Create `summaries` Table

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__create_summaries_table.sql`

**Depends on:** None

- [x] **Step 1:** Write Flyway migration V5:
  ```sql
  CREATE TABLE summaries (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      content_id INTEGER NOT NULL REFERENCES content(id) ON DELETE CASCADE,
      summary_type TEXT NOT NULL DEFAULT 'medium',
      summary TEXT,
      key_points TEXT,
      generated_at TEXT NOT NULL DEFAULT (datetime('now')),
      UNIQUE(content_id, summary_type)
  );
  CREATE INDEX idx_summaries_content_id ON summaries(content_id);
  ```
- [x] **Step 2:** Verify migration runs clean against a fresh SQLite DB (`./mvnw flyway:migrate`).

---

### Task 2: Summary Entity & Repository

**Files:**
- Create: `backend/src/main/java/com/knowvault/summary/Summary.java`
- Create: `backend/src/main/java/com/knowvault/summary/SummaryRepository.java`

**Depends on:** Task 1

- [x] **Step 1:** Create `Summary` JPA entity:
  - `id` (Long, `@Id @GeneratedValue IDENTITY`)
  - `content` — `@ManyToOne(fetch = LAZY)` with `@JoinColumn(name = "content_id")`, maps to existing `Content` entity
  - `summaryType` (String, `@Column(name = "summary_type")`, values: `short`/`medium`/`detailed`)
  - `summary` (String, `@Column(columnDefinition = "TEXT")`)
  - `keyPoints` (String, `@Column(name = "key_points", columnDefinition = "TEXT")`) — stored as JSON array string
  - `generatedAt` (String, `@Column(name = "generated_at")`) — ISO-8601 text, matches existing date pattern used by `Content`
  - Use `@Table(name = "summaries")` with `@Getter @Setter @NoArgsConstructor` (matches project Lombok convention).
- [x] **Step 2:** Create `SummaryRepository` extending `JpaRepository<Summary, Long>`:
  - `List<Summary> findByContentId(Long contentId)`
  - `Optional<Summary> findByContentIdAndSummaryType(Long contentId, String summaryType)`
  - `void deleteByContentId(Long contentId)`

---

### Task 3: Add AI Configuration Properties

**Files:**
- Modify: `backend/src/main/resources/application.yml`

**Depends on:** None

- [x] **Step 1:** Append `ai` block under existing `knowvault:` section:
  ```yaml
  knowvault:
    # ...existing import block...
    ai:
      provider: none            # none | gemini | ollama
      gemini:
        api-key: ""
        model: gemini-2.0-flash
      ollama:
        base-url: http://localhost:11434
        model: llama3
  ```
  Default `provider: none` ensures the app works without any AI setup (SRS AI-001: No AI Mode). API key is local-only, never transmitted to our server (SRS AI-002).

---

### Task 4: AI Provider Abstraction

**Files:**
- Create: `backend/src/main/java/com/knowvault/ai/AIProvider.java` (interface)
- Create: `backend/src/main/java/com/knowvault/ai/GeminiProvider.java`
- Create: `backend/src/main/java/com/knowvault/ai/OllamaProvider.java`
- Create: `backend/src/main/java/com/knowvault/ai/NoOpProvider.java`
- Create: `backend/src/main/java/com/knowvault/ai/AIConfig.java`

**Depends on:** Task 3

- [x] **Step 1:** Create `AIProvider` interface with method: `AIResponse summarize(String content, SummaryLength length)` — returns a DTO with title, summary, keyPoints (List\<String\>), suggestedTags (List\<String\>)
- [x] **Step 2:** Create `GeminiProvider` — uses `WebClient` to call Gemini API (`https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`). Reads `api-key`, `model` from config. Builds prompt requesting structured JSON output. Parses JSON response. Configure 10-second timeout.
- [x] **Step 3:** Create `OllamaProvider` — uses `RestTemplate` to call Ollama `POST http://localhost:11434/api/generate`. Reads `base-url`, `model` from config. Same prompt structure as Gemini. Configure 10-second timeout.
- [x] **Step 4:** Create `NoOpProvider` — returns a placeholder response: "AI not configured. Set knowvault.ai.provider to gemini or ollama."
- [x] **Step 5:** Create `AIConfig` `@Configuration` that reads `knowvault.ai.provider` (none/gemini/ollama) and returns the appropriate `AIProvider` bean via `@ConditionalOnProperty` or manual switch.

---

### Task 5: Summary Service

**Files:**
- Create: `backend/src/main/java/com/knowvault/summary/dto/AIResponse.java`
- Create: `backend/src/main/java/com/knowvault/summary/dto/SummaryResponse.java`
- Create: `backend/src/main/java/com/knowvault/summary/SummaryService.java`

**Depends on:** Tasks 2, 4

- [x] **Step 1:** Create `AIResponse` DTO — fields: `title` (String), `summary` (String), `keyPoints` (List\<String\>), `suggestedTags` (List\<String\>). Add Lombok `@Data` / `@Builder`.
- [x] **Step 2:** Create `SummaryResponse` DTO for API output — fields: `id`, `contentId`, `summaryType` (String), `summary` (String), `keyPoints` (List\<String\>), `generatedAt` (String).
- [x] **Step 3:** Create `SummaryService` with:
  - `generateSummary(contentId, summaryType)` — fetches `Content` from DB, calls `AIProvider.summarize()` with `contentText` and length enum, persists result to `summaries` table, returns `SummaryResponse`.
  - `getSummary(contentId, summaryType)` — fetches existing summary from DB, returns `SummaryResponse` or empty.
  - `getSummaries(contentId)` — returns all summaries for a content item as `List<SummaryResponse>`.

**Prompt design:** Prompt must instruct the AI to return valid JSON with keys `title`, `summary`, `key_points`, `tags`. Summary length mapping:
- Short → "2-3 sentences"
- Medium → "one detailed paragraph"
- Detailed → "multiple paragraphs with bullet-point key takeaways"

**Error handling:** Wrap AI calls in try/catch; on failure, log error and throw runtime exception with user-friendly message. Enforce 10-second timeout via WebClient/RestTemplate config.

---

### Task 6: Summary Controller

**Files:**
- Create: `backend/src/main/java/com/knowvault/summary/SummaryController.java`

**Depends on:** Task 5

- [x] **Step 1:** Create `@RestController` at `/api/content/{contentId}/summaries`:
  - `POST /` — accepts `{ "type": "short" | "medium" | "detailed" }`, calls `SummaryService.generateSummary()`, returns `201` with `SummaryResponse`.
  - `GET /` — calls `SummaryService.getSummaries()`, returns `List<SummaryResponse>`.
  - `GET /{type}` — calls `SummaryService.getSummary()`, returns `SummaryResponse` or `404`.

---

### Task 7: Summary TypeScript Types

**Files:**
- Create: `frontend/src/types/summary.ts`
- Modify: `frontend/src/types/index.ts`

**Depends on:** None

- [x] **Step 1:** Create `SummaryType = 'short' | 'medium' | 'detailed'` union type (matches `content.ts` pattern of exporting standalone type aliases)
- [x] **Step 2:** Create `Summary` interface: `id: number`, `contentId: number`, `summaryType: SummaryType`, `summary: string`, `keyPoints: string[]`, `generatedAt: string`
- [x] **Step 3:** Create `SummaryGenerateRequest` interface: `type: SummaryType` (request body for the generate endpoint)
- [x] **Step 4:** Add `export type { Summary, SummaryType, SummaryGenerateRequest } from './summary'` to `types/index.ts`

---

### Task 8: Summary API Service

**Files:**
- Create: `frontend/src/services/summaryService.ts`
- Modify: `frontend/src/services/index.ts`

**Depends on:** Task 7

- [x] **Step 1:** Import `api` from `./api` and types from `@/types` (same pattern as `contentService.ts`)
- [x] **Step 2:** Create `generateSummary(contentId: number, type: SummaryType): Promise<Summary>` — `POST /content/${contentId}/summaries` with body `{ type }`, return `response.data`
- [x] **Step 3:** Create `getSummaries(contentId: number): Promise<Summary[]>` — `GET /content/${contentId}/summaries`, return `response.data`
- [x] **Step 4:** Create `getSummary(contentId: number, type: SummaryType): Promise<Summary>` — `GET /content/${contentId}/summaries/${type}`, return `response.data`
- [x] **Step 5:** Add `export * as summaryService from './summaryService'` to `services/index.ts`

---

### Task 9: useSummary Hook

**Files:**
- Create: `frontend/src/hooks/useSummary.ts`
- Modify: `frontend/src/hooks/index.ts`

**Depends on:** Tasks 7, 8

- [x] **Step 1:** Create `useSummary(contentId: number)` hook with return type: `summaries: Summary[]`, `loading: boolean`, `error: string | null`, `generating: boolean`, `generate(type: SummaryType): Promise<void>`, `refetch(): void`
- [x] **Step 2:** Follow `useContent.ts` patterns exactly: `mountedRef` via `useRef(true)`, `useState` for data/loading/error/`generating`, `useCallback` for `fetchSummaries` and `generate`, `useEffect` to invoke `fetchSummaries` on mount with cleanup `mountedRef.current = false`
- [x] **Step 3:** `fetchSummaries`: call `summaryService.getSummaries(contentId)`, guard with `mountedRef.current`, set `summaries` / `error` / `loading`
- [x] **Step 4:** `generate(type)`: set `generating = true`, call `summaryService.generateSummary(contentId, type)`, then call `fetchSummaries()` to refresh list, guard all state updates with `mountedRef`
- [x] **Step 5:** Export `useSummary` from `hooks/index.ts`

---

### Task 10: SummaryCard Component

**Files:**
- Create: `frontend/src/components/ui/SummaryCard.tsx`

**Depends on:** Task 9

- [x] **Step 1:** Create `SummaryCard` component receiving a `Summary` object. Render a summary type badge (short=info, medium=warning, detailed=success), the summary text, key points as a bulleted list, and a generated-at timestamp formatted with `toLocaleDateString`.
- [x] **Step 2:** Style with Tailwind: `p-4` card padding, `text-sm leading-relaxed` for summary text, `list-disc pl-5 space-y-1 mt-3` for key points, `text-xs text-[var(--muted-foreground)] mt-3` for timestamp.
- [x] **Step 3:** Wrap content in existing `Card` component. Use `Badge` with appropriate variant for the type indicator. Key points title uses `font-medium text-[var(--foreground)]`.

---

### Task 11: SummarySection Component

**Files:**
- Create: `frontend/src/components/summary/SummarySection.tsx`

**Depends on:** Tasks 9, 10

- [x] **Step 1:** Create `SummarySection` taking `contentId: number`. Render header with "AI Summary" text and a generate button. Show three toggle buttons (`sm` size, `outline` variant) for short/medium/detailed summary types. Display the matching `SummaryCard` for the selected type, or `EmptyState` if no summary exists for that type.
- [x] **Step 2:** Use `useSummary(contentId)` for data. Show `Spinner` centered while `loading` is true. Render error message inline with `text-[var(--destructive)]`. Call `useToast()` to show success/error toasts after `generate()` resolves/rejects.
- [x] **Step 3:** Generate button uses `Button` variant `default`, size `sm`. Show `Spinner` inside the button (replacing icon) while `generating` is true. Set `disabled={generating}` on the button.
- [x] **Step 4:** When the backend returns no summaries and no error on generate attempt (no-op provider), display message: "Configure AI in Settings to enable summaries" styled with `text-[var(--muted-foreground)] text-sm`.

---

### Task 12: Integrate SummarySection into ContentDetail

**Files:**
- Modify: `frontend/src/pages/ContentDetail.tsx`

**Depends on:** Task 11

- [x] **Step 1:** Import `SummarySection` from `@/components/summary/SummarySection`.
- [x] **Step 2:** Place `<SummarySection>` below the content article in the left column, separated by `mt-6`. This positions it between the main content and the bottom of the page.
- [x] **Step 3:** Pass the existing `numericId` as `contentId` prop: `<SummarySection contentId={numericId} />`.

---

## Summary Table

| Task | Description | Files | Steps | Depends On |
|------|-------------|-------|-------|------------|
| 1 | DB Migration — `summaries` table | 1 create | 2 | — |
| 2 | Summary Entity & Repository | 2 create | 2 | 1 |
| 3 | AI Configuration Properties | 1 modify | 1 | — |
| 4 | AI Provider Abstraction | 5 create | 5 | 3 |
| 5 | Summary Service + DTOs | 3 create | 3 | 2, 4 |
| 6 | Summary Controller | 1 create | 1 | 5 |
| 7 | Summary TypeScript Types | 1 create, 1 modify | 4 | — |
| 8 | Summary API Service | 1 create, 1 modify | 5 | 7 |
| 9 | useSummary Hook | 1 create, 1 modify | 5 | 7, 8 |
| 10 | SummaryCard Component | 1 create | 3 | 9 |
| 11 | SummarySection Component | 1 create | 4 | 9, 10 |
| 12 | Integrate into ContentDetail | 1 modify | 3 | 11 |

**Totals:** 12 tasks · 18 files created · 3 files modified · 38 steps
