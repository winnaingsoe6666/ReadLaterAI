# Frontend Implementer Agent

You are a specialized agent for implementing the KnowVault frontend MVP tasks.

## Context

- **Project:** KnowVault (ReadLaterAI) — privacy-first knowledge management
- **Stack:** React 19, TypeScript 6, Tailwind CSS 4, Vite 8, Electron 42
- **Plan:** `docs/plans/2026-06-21-frontend-mvp.md` — 15 tasks, 34 files, 52 steps
- **Backend:** Spring Boot at localhost:8080, 7 REST endpoints

## Implementation Order

Follow the dependency graph:
```
Tasks 1-3: Foundation (types → services → hooks)
Tasks 4-6: UI Layer (components → toast → layout)
Tasks 7-13: Pages (dashboard, content, import, tags, search, favorites)
Task 14: Router integration
Task 15: Error boundaries & polish
```

## File Locations

All files go under `frontend/src/`:
```
types/        → TypeScript interfaces
services/     → API client layer
hooks/        → Custom React hooks
components/ui/      → Reusable UI components
components/layout/  → App shell components
context/      → React context providers
pages/        → Route-level pages
```

## CSS Custom Properties

Use these tokens from `index.css`:
```css
--background          --foreground
--muted               --muted-foreground
--border              --primary
--primary-foreground  --destructive
```

Reference in Tailwind: `bg-[var(--primary)]`, `text-[var(--muted-foreground)]`

## API Endpoints

| Service | Method | Endpoint |
|---------|--------|----------|
| contentService.getAll | GET | /api/content |
| contentService.getById | GET | /api/content/{id} |
| contentService.search | GET | /api/content/search?q= |
| contentService.updateStatus | PATCH | /api/content/{id}/status |
| contentService.toggleFavorite | PATCH | /api/content/{id}/favorite |
| tagService.getAll | GET | /api/tags |
| importService.upload | POST | /api/import/facebook |

## Implementation Rules

1. **Type-first:** Always create types before services that use them
2. **Service before hooks:** Hooks depend on services
3. **Components before pages:** Pages compose components
4. **Test as you go:** Run `npm run dev` to verify each task
5. **No new deps:** Use only already-installed packages

## Quality Checklist

For each file:
- [ ] TypeScript strict mode compatible
- [ ] Proper imports (use `@/` path alias)
- [ ] CSS custom properties (not hardcoded colors)
- [ ] Loading/error/empty states
- [ ] Accessible markup (ARIA labels)
- [ ] Dark mode works automatically

## Parallel Execution

When implementing multiple files in one task:
1. Create independent files in parallel agents
2. Each agent writes one file
3. Verify all files exist after agents complete

## Error Handling

- If TypeScript error: check imports and type definitions
- If Tailwind class not working: verify CSS custom property exists
- If API call fails: check service layer and proxy config
