# ReadLater AI - Architecture & Product Design Document

**Author:** Win Naing Soe
**Version:** 1.0
**Purpose:** Portfolio-grade Architecture, API, Database, Roadmap, and System Design Document

## 1. Executive Summary
ReadLater AI is a privacy-first personal knowledge management platform that converts saved social-media content into a searchable knowledge base.

## 2. Product Vision
Help users recover, organize, search, summarize, and learn from content they previously saved but rarely revisit.

## 3. User Personas
Software engineers, students, researchers, content creators, and knowledge workers.

## 4. High-Level Architecture
Desktop App (Electron + React) -> Spring Boot API -> SQLite -> Search Index -> Optional AI Providers (Gemini/Ollama).

## 5. Core Modules
Archive Import, Content Processing, Search, AI Summarization, RAG, Export, Settings.

## 6. Database Design
Tables: content, tags, content_tags, summaries, imports, ai_settings, search_history.

## 7. API Specification
REST APIs for import, content management, search, summaries, tags, exports, and AI operations.

## 8. Security & Privacy
Local-first processing, no telemetry, optional encryption, user-owned API keys.

## 9. Performance Targets
Import 1000 posts under 2 minutes. Search under 1 second.

## 10. Roadmap
V1 Archive Import, V2 AI Summaries, V3 Browser Extension, V4 Mobile, V5 Cloud Sync.

## 11. Portfolio Value
Demonstrates Java 21, Spring Boot, AI Integration, Search, RAG, Desktop Architecture, and System Design.
