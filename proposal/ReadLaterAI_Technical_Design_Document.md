# ReadLaterAI

## Technical Design Document (TDD)

Version: 1.0

Author: Win Naing Soe

Project Type: Desktop Application (Local First)

## Tech Stack

- Java 21
- Spring Boot 3
- React
- Electron
- SQLite
- Gemini API (Optional)
- Ollama (Optional)

---

# Product Overview

KnowVault is a privacy-first personal knowledge management platform.

Users import Facebook archives and other saved content.

The system transforms saved content into searchable knowledge.

No cloud account required.

No subscription required.

User owns all data.

---

# Installation Flow

## Welcome Screen

Welcome to KnowVault

Your private knowledge vault.

[Install]

## Installation Steps

1. Check Operating System
2. Select Installation Folder
3. Create Data Directory
4. Create SQLite Database
5. Install Search Components
6. Create Desktop Shortcut
7. Launch Application

---

# First Launch Flow

## Welcome

Build your personal knowledge vault.

Import Existing Data?

(Recommended)

[Import Facebook Archive]

[Skip]

## Import Wizard

1. Select ZIP file
2. Configure archive settings
3. Review import summary
4. Import
5. View progress
6. Open dashboard

---

# Main Navigation

- Dashboard
- Inbox
- Categories
- Tags
- Search
- Favorites
- Imports
- Settings
- Help

---

# Modules

## Import Module

- Validate ZIP
- Extract Files
- Parse Content
- Normalize Records
- Store Database
- Build Search Index

## Search Module

- SQLite FTS5
- Semantic Search (Future)

## AI Module

- No AI
- Gemini
- Ollama

---

# Future Roadmap

## Phase 1
- Import
- Search
- Categories
- Tags

## Phase 2
- Gemini AI

## Phase 3
- Chrome Extension

## Phase 4
- Android App

## Phase 5
- RAG Assistant

