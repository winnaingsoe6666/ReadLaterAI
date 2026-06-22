# ReadLaterAI (KnowVault)

A privacy-first personal knowledge management platform that converts saved social-media content into a searchable knowledge base. Import your Facebook archive, get AI-powered summaries, and organize everything with tags and categories.

## Features

- **Facebook Archive Import** — Import posts, saved items, Messenger links, group posts, comments, and more from your Facebook data export
- **AI Summaries** — Generate short, medium, or detailed summaries of your content using Google Gemini or local Ollama
- **Full-Text Search** — SQLite FTS-powered search across all your imported content
- **Tags & Categories** — Auto-tagged and categorized content (technology, food, news, entertainment, health, education)
- **Favorites & Status Tracking** — Mark items as unread, reading, or completed
- **Desktop App** — Electron wrapper for Windows, Mac, and Linux
- **Privacy-First** — All data stored locally in SQLite. No cloud dependency.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3, JPA/Hibernate, Flyway |
| Database | SQLite |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS |
| Desktop | Electron |
| AI | Google Gemini, Ollama (local) |

## Prerequisites

- **Java 21+** — `java -version` to check
- **Node.js 20+** — `node -version` to check
- **Git** — for cloning

## Quick Start

```bash
# 1. Clone the repository
git clone https://github.com/winnaingsoe6666/ReadLaterAI.git
cd ReadLaterAI

# 2. Start both frontend and backend
cd frontend
npm install
npm run dev
```

This starts:
- **Backend** (Spring Boot) on `http://localhost:8080`
- **Frontend** (Vite dev server) on `http://localhost:5173`

Open http://localhost:5173 in your browser.

## Manual Setup (Separate Terminals)

### Backend

```bash
cd backend

# Build and run
./mvnw spring-boot:run

# Or build a JAR
./mvnw clean package -DskipTests
java -jar target/knowvault-backend-0.0.1-SNAPSHOT.jar
```

The backend runs on `http://localhost:8080`. Database is auto-created at `backend/data/knowvault.db` with Flyway migrations.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173` and proxies API requests to the backend.

## Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
knowvault:
  ai:
    provider: gemini          # Options: gemini, ollama, none
    gemini:
      api-key: YOUR_KEY       # Get from https://aistudio.google.com/apikey
      model: gemini-2.0-flash
    ollama:
      base-url: http://localhost:11434
      model: llama3
```

### AI Provider Setup

| Provider | Setup |
|----------|-------|
| **Gemini** | Get a free API key from [Google AI Studio](https://aistudio.google.com/apikey), set `provider: gemini` |
| **Ollama** | Install [Ollama](https://ollama.com), pull a model (`ollama pull llama3`), set `provider: ollama` |
| **None** | Set `provider: none` — summaries will show a placeholder message |

## Usage

### Import Facebook Archive

1. Go to [Facebook Settings → Your Information](https://www.facebook.com/settings/your_information) → **Download Your Information**
2. Select **JSON** format and the data you want (posts, saved items, comments, etc.)
3. Download the ZIP file
4. Open KnowVault → **Import** page → upload the ZIP

**Supported data types:**
- Posts (your own posts)
- Saved items
- Messenger shared links
- Group posts and comments
- Your comments on posts
- Liked pages and ad preferences (stored as metadata)

### Generate AI Summaries

1. Go to **Settings** → configure your AI provider
2. Open any content item → click **Generate** in the AI Summary section
3. Choose short, medium, or detailed summary

### Search

Use `Ctrl+K` (or `Cmd+K` on Mac) to open the search bar. Full-text search across all imported content.

## Build Desktop App (Electron)

```bash
cd frontend

# Windows
npm run package:win

# Mac
npm run package:mac

# Linux
npm run package:linux
```

Output is in `frontend/dist-electron/`.

## Running Tests

```bash
cd backend
mvn test
```

## Project Structure

```
ReadLaterAI/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/knowvault/
│   │   ├── import_/            # Facebook archive parser & import service
│   │   ├── content/            # Content entity & repository
│   │   ├── tag/                # Tag system
│   │   ├── summary/            # AI summary service
│   │   └── ai/                 # AI providers (Gemini, Ollama)
│   └── src/main/resources/
│       ├── application.yml     # Main config
│       └── db/migration/       # Flyway SQL migrations
├── frontend/                   # React + Vite application
│   ├── src/
│   │   ├── pages/              # Route pages
│   │   ├── components/         # UI components
│   │   ├── hooks/              # React hooks
│   │   ├── services/           # API clients
│   │   └── types/              # TypeScript types
│   └── electron/               # Electron wrapper
├── scripts/                    # Build scripts
└── docs/                       # Documentation & plans
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/content` | List all content |
| GET | `/api/content/{id}` | Get content by ID |
| PATCH | `/api/content/{id}` | Update content (status, favorite) |
| DELETE | `/api/content/{id}` | Delete content |
| POST | `/api/import/facebook` | Import Facebook archive ZIP |
| POST | `/api/content/{id}/summaries` | Generate AI summary |
| GET | `/api/content/{id}/summaries` | Get summaries for content |
| GET | `/api/tags` | List all tags |
| GET | `/api/content/search?q=query` | Full-text search |

## License

Private project.
