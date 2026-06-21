# KnowVault Frontend MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the KnowVault frontend MVP — a complete React UI for all existing backend capabilities including content browsing, search, import, tags, and favorites.

**Architecture:** React 19 SPA inside Electron 42 desktop shell. Feature-based directory structure with shared UI components, API service layer, and custom hooks.

**Tech Stack:** React 19, TypeScript 6, Vite 8, Tailwind CSS 4, Electron 42, Axios, React Router DOM 7, Lucide React

## Global Constraints

- TypeScript strict mode enabled
- No additional npm dependencies beyond what's already installed
- All styling via Tailwind CSS utility classes with CSS custom properties
- API calls go through `/api` prefix (Vite proxy to `http://localhost:8080`)
- Components use `var(--token)` syntax for theme tokens defined in `index.css`
- Dark mode via `@media (prefers-color-scheme: dark)` already configured

## MVP Scope

### In Scope
- API service layer (axios client with interceptors)
- TypeScript types matching backend entities
- Custom React hooks for data fetching
- Reusable UI components (Button, Card, Input, Badge, Modal, Toast, Spinner, EmptyState)
- Layout with collapsible sidebar navigation
- 7 pages: Dashboard, ContentList, ContentDetail, Import, Tags, Search, Favorites
- Error boundaries and loading states
- Toast notification system

### Out of Scope
- AI features (summarization, recommendations)
- Export (PDF, Markdown)
- Settings page
- Chrome extension
- Mobile app
- Authentication
- Offline/PWA

## Directory Structure

```
frontend/src/
├── types/
│   ├── content.ts
│   ├── tag.ts
│   ├── import.ts
│   └── index.ts
├── services/
│   ├── api.ts
│   ├── contentService.ts
│   ├── tagService.ts
│   ├── importService.ts
│   └── index.ts
├── hooks/
│   ├── useContent.ts
│   ├── useTags.ts
│   └── useImport.ts
├── components/
│   ├── ui/
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── Input.tsx
│   │   ├── Badge.tsx
│   │   ├── Modal.tsx
│   │   ├── Toast.tsx
│   │   ├── Spinner.tsx
│   │   ├── EmptyState.tsx
│   │   └── ContentCard.tsx
│   ├── layout/
│   │   ├── AppLayout.tsx
│   │   ├── Sidebar.tsx
│   │   └── Header.tsx
│   └── ErrorBoundary.tsx
├── context/
│   └── ToastContext.tsx
├── pages/
│   ├── Dashboard.tsx
│   ├── ContentList.tsx
│   ├── ContentDetail.tsx
│   ├── ImportPage.tsx
│   ├── TagsPage.tsx
│   ├── SearchPage.tsx
│   └── FavoritesPage.tsx
├── App.tsx
├── main.tsx
└── index.css
```

## Dependency Graph

```
Task 1: Types           ──┐
Task 2: API Services    ──┤── Task 3: Hooks ──┐
                          │                    │
Task 4: UI Components   ──┤── Task 5: Toast   │
                          │── Task 6: Layout   │
                          │                    │
                          └── Tasks 7-13: Pages (depend on 3,4,6)
                               │
                          Task 14: Router (depends on all pages)
                               │
                          Task 15: Error Boundaries & Polish
```

---
### Task 1: TypeScript Types

**Files:**
- Create: `frontend/src/types/content.ts`
- Create: `frontend/src/types/tag.ts`
- Create: `frontend/src/types/import.ts`
- Create: `frontend/src/types/index.ts`

**Depends on:** none

- [ ] **Step 1:** Create `frontend/src/types/content.ts` with:
  - `ContentStatus` union type: `'unread' | 'reading' | 'completed'`
  - `ContentFilter` type: `'all' | ContentStatus`
  - `Content` interface: `{ id: number; title: string; contentText: string; url: string; source: string; category: string; author: string; createdDate: string; importDate: string; status: ContentStatus; favorite: boolean; tags: Tag[] }`
  - Import `Tag` from `./tag` using `import type`

- [ ] **Step 2:** Create `frontend/src/types/tag.ts` with:
  - `Tag` interface: `{ id: number; name: string }`
  - `TagWithCount` interface extending Tag: `{ contentCount: number }`

- [ ] **Step 3:** Create `frontend/src/types/import.ts` with:
  - `ImportResult` interface: `{ imported: number; skipped: number; total: number }`

- [ ] **Step 4:** Create `frontend/src/types/index.ts` barrel export:
  - `export type { Content, ContentStatus, ContentFilter } from './content'`
  - `export type { Tag, TagWithCount } from './tag'`
  - `export type { ImportResult } from './import'`

---

### Task 2: API Service Layer

**Files:**
- Create: `frontend/src/services/api.ts`
- Create: `frontend/src/services/contentService.ts`
- Create: `frontend/src/services/tagService.ts`
- Create: `frontend/src/services/importService.ts`
- Create: `frontend/src/services/index.ts`

**Depends on:** Task 1

- [ ] **Step 1:** Create `frontend/src/services/api.ts`:
  - Create axios instance with `baseURL: '/api'`
  - Response interceptor: extract error message from `error.response.data.message`, log to console, re-throw
  - Network error interceptor: throw "No response received from server"
  - Export as default

- [ ] **Step 2:** Create `frontend/src/services/contentService.ts`:
  - `getAll(): Promise<Content[]>` — `GET /content`
  - `getById(id: number): Promise<Content>` — `GET /content/${id}`
  - `search(query: string): Promise<Content[]>` — `GET /content/search` with `params: { q: query }`
  - `updateStatus(id: number, status: ContentStatus): Promise<Content>` — `PATCH /content/${id}/status` with body `{ status }`
  - `toggleFavorite(id: number): Promise<Content>` — `PATCH /content/${id}/favorite`

- [ ] **Step 3:** Create `frontend/src/services/tagService.ts`:
  - `getAll(): Promise<Tag[]>` — `GET /tags`

- [ ] **Step 4:** Create `frontend/src/services/importService.ts`:
  - `uploadFacebookArchive(file: File, deleteAfterImport?: boolean): Promise<ImportResult>` — `POST /import/facebook` with FormData, multipart header

- [ ] **Step 5:** Create `frontend/src/services/index.ts` barrel export

---

### Task 3: Custom Hooks

**Files:**
- Create: `frontend/src/hooks/useContent.ts`
- Create: `frontend/src/hooks/useTags.ts`
- Create: `frontend/src/hooks/useImport.ts`

**Depends on:** Task 2

- [ ] **Step 1:** Create `frontend/src/hooks/useContent.ts`:
  - `useContentList()` — fetches all content on mount, returns `{ content, loading, error, refetch }`
  - `useContentDetail(id)` — fetches single item, returns `{ item, loading, error, refetch }`
  - `useContentSearch(query)` — debounced search (300ms), returns `{ results, loading, error }`
  - `useContentActions()` — returns `{ updateStatus, toggleFavorite }` with optimistic updates

- [ ] **Step 2:** Create `frontend/src/hooks/useTags.ts`:
  - `useTags()` — fetches tags + all content to derive counts, returns `{ tags: TagWithCount[], loading, error, refetch }`

- [ ] **Step 3:** Create `frontend/src/hooks/useImport.ts`:
  - `useImport()` — state machine (idle/uploading/success/error), returns `{ importFile, status, result, error, reset }`

---

### Task 4: UI Primitive Components

**Files:**
- Create: `frontend/src/components/ui/Button.tsx`
- Create: `frontend/src/components/ui/Card.tsx`
- Create: `frontend/src/components/ui/Input.tsx`
- Create: `frontend/src/components/ui/Badge.tsx`
- Create: `frontend/src/components/ui/Modal.tsx`
- Create: `frontend/src/components/ui/Spinner.tsx`
- Create: `frontend/src/components/ui/EmptyState.tsx`
- Create: `frontend/src/components/ui/ContentCard.tsx`

**Depends on:** none

- [ ] **Step 1:** Create `Button.tsx`:
  - Props: `variant: 'primary' | 'secondary' | 'ghost' | 'destructive'`, `size?: 'sm' | 'md' | 'lg'`, `disabled?`, `onClick?`, `children`, `className?`
  - Variants: primary=`bg-[var(--primary)] text-[var(--primary-foreground)]`, secondary=`bg-[var(--muted)]`, ghost=`bg-transparent hover:bg-[var(--muted)]`, destructive=`bg-[var(--destructive)] text-white`

- [ ] **Step 2:** Create `Card.tsx`:
  - Props: `className?`, `hover?`, `children`
  - Renders: `rounded-xl border border-[var(--border)] bg-[var(--background)] p-4`
  - Optional hover state with primary border

- [ ] **Step 3:** Create `Input.tsx`:
  - Props: `label?`, `error?`, `icon?`, plus standard input HTML attributes
  - Renders label, optional icon, input with border, error text below

- [ ] **Step 4:** Create `Badge.tsx`:
  - Props: `variant: 'default' | 'success' | 'warning' | 'info' | 'destructive'`, `children`
  - Renders pill with variant-specific colors

- [ ] **Step 5:** Create `Modal.tsx`:
  - Props: `open`, `onClose`, `title?`, `children`
  - Fixed overlay with backdrop, Escape key listener, close on backdrop click

- [ ] **Step 6:** Create `Spinner.tsx`:
  - Props: `size?: 'sm' | 'md' | 'lg'`
  - Animated spinner using lucide-react Loader2 icon

- [ ] **Step 7:** Create `EmptyState.tsx`:
  - Props: `icon`, `title`, `description`, `action?: { label, onClick }`
  - Centered layout with icon, text, optional CTA button

- [ ] **Step 8:** Create `ContentCard.tsx`:
  - Props: `item: Content`, `onFavoriteToggle?`, `compact?`
  - Reusable card for content items with title, category badge, status badge, favorite toggle
  - Compact mode for Dashboard (hides source)

---

### Task 5: Toast Context

**Files:**
- Create: `frontend/src/context/ToastContext.tsx`
- Create: `frontend/src/components/ui/Toast.tsx`
- Modify: `frontend/src/main.tsx`

**Depends on:** Task 4

- [ ] **Step 1:** Create `ToastContext.tsx`:
  - Toast type: `{ id: string; message: string; type: 'success' | 'error' | 'info' }`
  - `ToastProvider` manages toast array, auto-dismiss after 3s
  - `useToast()` hook returns `{ addToast, removeToast }`

- [ ] **Step 2:** Create `Toast.tsx`:
  - Individual toast display with icon per type, close button, auto-dismiss timer
  - Fixed position bottom-right

- [ ] **Step 3:** Modify `main.tsx`:
  - Wrap `<App />` with `<ToastProvider>`

---

### Task 6: Layout Components

**Files:**
- Create: `frontend/src/components/layout/AppLayout.tsx`
- Create: `frontend/src/components/layout/Sidebar.tsx`
- Create: `frontend/src/components/layout/Header.tsx`

**Depends on:** Task 4

- [ ] **Step 1:** Create `Sidebar.tsx`:
  - Props: `collapsed: boolean`, `onToggle: () => void`
  - Nav links: Dashboard(/), Content(/content), Tags(/tags), Search(/search), Favorites(/favorites), Import(/import)
  - Active state via NavLink, collapse toggle, KnowVault logo

- [ ] **Step 2:** Create `Header.tsx`:
  - Props: `sidebarCollapsed: boolean`
  - Dynamic page title from route, global search input with Cmd+K shortcut
  - App version display (Electron only)

- [ ] **Step 3:** Create `AppLayout.tsx`:
  - Full-height flex: Sidebar + (Header + Outlet)
  - Manages sidebarCollapsed state

---

### Task 7: Dashboard Page

**Files:**
- Modify: `frontend/src/pages/Dashboard.tsx`

**Depends on:** Task 3, Task 4, Task 6

- [ ] **Step 1:** Replace static Dashboard. Remove hardcoded content. Import `useContentList`, UI components, and icons (BookOpen, FileText, Heart, Tag, Upload, Search).

- [ ] **Step 2:** Implement stats section:
  - Derive from content: total count, unread count, favorites count, category count
  - Render 4 Card components in responsive grid (`grid-cols-2 md:grid-cols-4 gap-4`)

- [ ] **Step 3:** Implement recent content (last 10 by importDate) as ContentCard list with compact mode.
  - Quick actions row: Import, Search, Browse Content buttons
  - Loading: Spinner. Empty: EmptyState with CTA to /import

---

### Task 8: ContentList Page

**Files:**
- Create: `frontend/src/pages/ContentList.tsx`

**Depends on:** Task 3, Task 4, Task 6

- [ ] **Step 1:** Create page with `useContentList()`. Set up state: statusFilter, categoryFilter, sortBy.

- [ ] **Step 2:** Implement filter controls:
  - Status tabs: All, Unread, Reading, Completed
  - Category dropdown from unique categories
  - Sort dropdown: Date (newest/oldest), Title (A-Z/Z-A)

- [ ] **Step 3:** Render filtered/sorted ContentCard list. Favorite toggle calls `contentService.toggleFavorite` then refetch. Loading/empty states.

---

### Task 9: ContentDetail Page

**Files:**
- Create: `frontend/src/pages/ContentDetail.tsx`

**Depends on:** Task 3, Task 4, Task 6

- [ ] **Step 1:** Read `id` from useParams. Fetch with `useContentDetail(Number(id))`. Back button with ArrowLeft icon.

- [ ] **Step 2:** Two-column layout (lg:grid-cols-3):
  - Left (col-span-2): rendered contentText via dangerouslySetInnerHTML
  - Right (col-span-1): metadata Card with title, author, source link, date, category Badge, status dropdown, favorite toggle, tag Badges

- [ ] **Step 3:** Loading: Spinner. Error: EmptyState "Content not found" with link to /content.

---

### Task 10: Import Page

**Files:**
- Create: `frontend/src/pages/ImportPage.tsx`

**Depends on:** Task 3, Task 4, Task 6

- [ ] **Step 1:** Create page with `useImport()`. Add drag-and-drop state (isDragging).

- [ ] **Step 2:** Idle state: dashed border drop zone with Upload icon. File input (accept=.zip). Drag/drop handlers. Client-side validation (.zip only).

- [ ] **Step 3:** Success state: summary card with imported/skipped/total counts. "Import Another" and "View Content" buttons. Error state: message + "Try Again" button.

---

### Task 11: Tags Page

**Files:**
- Create: `frontend/src/pages/TagsPage.tsx`

**Depends on:** Task 3, Task 4, Task 6

- [ ] **Step 1:** Create page with `useTags()`. Read `?filter` from useSearchParams. Add search text state.

- [ ] **Step 2:** Filter tags by search text. Responsive grid (grid-cols-2 sm:3 md:4 lg:6). Each tag Card shows name + count. Click navigates to /content?tag={name}. Highlight tag matching URL filter.

- [ ] **Step 3:** Loading: Spinner. Empty: EmptyState "No tags yet". Search no match: "No tags matching '{text}'".

---

### Task 12: Search Page

**Files:**
- Create: `frontend/src/pages/SearchPage.tsx`

**Depends on:** Task 3, Task 4, Task 6

- [ ] **Step 1:** Create page. Read `q` from useSearchParams. State for query, results, loading, error.

- [ ] **Step 2:** Debounced search (300ms via useDeferredValue or setTimeout). Call `contentService.search(query)`. Update URL params. Render results as ContentCard list.

- [ ] **Step 3:** Empty states: no query = search tips; no results = "No results for '{query}'". Loading: Spinner.

---

### Task 13: Favorites Page

**Files:**
- Create: `frontend/src/pages/FavoritesPage.tsx`

**Depends on:** Task 3, Task 4, Task 6

- [ ] **Step 1:** Create page with `useFavorites()` hook (getAll + filter favorite===true).

- [ ] **Step 2:** Sort dropdown (Date newest, Title A-Z). Render ContentCard list with onFavoriteToggle callback.

- [ ] **Step 3:** Loading: Spinner. Empty: EmptyState "No favorites yet" with Heart icon.

---

### Task 14: Router Update

**Files:**
- Modify: `frontend/src/App.tsx`

**Depends on:** Tasks 7-13

- [ ] **Step 1:** Replace router. Remove single Dashboard route. Add AppLayout wrapper with Outlet:
  - `<Route index element={<Dashboard />} />`
  - `<Route path="content" element={<ContentList />} />`
  - `<Route path="content/:id" element={<ContentDetail />} />`
  - `<Route path="tags" element={<TagsPage />} />`
  - `<Route path="search" element={<SearchPage />} />`
  - `<Route path="favorites" element={<FavoritesPage />} />`
  - `<Route path="import" element={<ImportPage />} />`

- [ ] **Step 2:** Add catch-all: `<Route path="*" element={<Navigate to="/" replace />} />`

---

### Task 15: Error Boundaries & Polish

**Files:**
- Create: `frontend/src/components/ErrorBoundary.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/index.css`

**Depends on:** Task 14

- [ ] **Step 1:** Create ErrorBoundary class component. Catches render errors. Shows fallback UI with AlertTriangle icon, "Something went wrong", reload button.

- [ ] **Step 2:** Wrap `<BrowserRouter>` with `<ErrorBoundary>` in App.tsx.

- [ ] **Step 3:** Add to index.css:
  - `--color-surface: var(--background)` alias
  - Custom scrollbar styles
  - Selection highlight with primary color
  - `scroll-behavior: smooth`

---

## Summary

| Task | Files | Steps |
|------|-------|-------|
| 1. Types | 4 create | 4 |
| 2. API Services | 5 create | 5 |
| 3. Hooks | 3 create | 3 |
| 4. UI Components | 8 create | 8 |
| 5. Toast Context | 2 create, 1 modify | 3 |
| 6. Layout | 3 create | 3 |
| 7. Dashboard | 1 modify | 3 |
| 8. ContentList | 1 create | 3 |
| 9. ContentDetail | 1 create | 3 |
| 10. Import | 1 create | 3 |
| 11. Tags | 1 create | 3 |
| 12. Search | 1 create | 3 |
| 13. Favorites | 1 create | 3 |
| 14. Router | 1 modify | 2 |
| 15. Polish | 1 create, 2 modify | 3 |
| **Total** | **34 files** | **52 steps** |
