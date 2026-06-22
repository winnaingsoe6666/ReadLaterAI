# Electron Wrapper — Desktop App Packaging Plan

**Project:** ReadLaterAI / KnowVault
**Date:** 2026-06-21
**Status:** Draft
**Depends on:** Frontend (React) — must be built first or in parallel

---

## 1. Goal

Package the ReadLaterAI application as a standalone desktop app using Electron. The app bundles:

- **Electron shell** — window management, native menus, tray, auto-update
- **React frontend** — the UI (built with Vite + TypeScript)
- **Spring Boot backend** — bundled as an executable JAR, spawned as a child process

The user launches a single desktop app. No separate terminal, no browser, no manual Java setup.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                 Electron Desktop App             │
│                                                  │
│  ┌──────────────┐     ┌──────────────────────┐  │
│  │  Main Process │     │   Renderer Process   │  │
│  │  (Node.js)    │     │   (React + Vite)     │  │
│  │               │     │                      │  │
│  │  - Window mgmt│ IPC │  - UI components     │  │
│  │  - Backend    │◄───►│  - API calls to      │  │
│  │    lifecycle  │     │    localhost:8080     │  │
│  │  - Auto-update│     │                      │  │
│  │  - Native OS  │     │                      │  │
│  └──────┬───────┘     └──────────────────────┘  │
│         │                                        │
│         │ spawn (child_process)                  │
│         ▼                                        │
│  ┌──────────────────────────────────────────┐   │
│  │         Spring Boot Backend (JAR)        │   │
│  │         localhost:8080                   │   │
│  │         SQLite: ./data/knowvault.db      │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

**Key design decisions:**

| Decision | Choice | Rationale |
|---|---|---|
| Frontend bundler | Vite | Fast HMR, native ESM, excellent React/TS support |
| Electron framework | electron-builder | Mature, supports NSIS/DMG/AppImage, code signing, auto-update |
| Backend packaging | Executable Spring Boot JAR | `mvn package` produces a single JAR; no separate JRE install needed |
| JRE bundling | JRE 21 bundled inside app | Users don't need Java installed; ~40MB added to package |
| Dev experience | Concurrent Vite dev server + Spring Boot | Hot reload for frontend, Spring DevTools for backend |
| Process management | Main process spawns JAR child | Clean lifecycle: start on app ready, kill on app quit |

---

## 3. Directory Structure

```
ReadLaterAI/
├── backend/                          # Existing Spring Boot project
│   ├── pom.xml
│   └── src/
├── frontend/                         # NEW — React + Vite + TypeScript
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   ├── electron/
│   │   ├── main.ts                   # Electron main process
│   │   ├── preload.ts                # Preload script (contextBridge)
│   │   └── ipc-handlers.ts           # IPC handlers (backend lifecycle, dialogs)
│   ├── src/
│   │   ├── main.tsx                  # React entry
│   │   ├── App.tsx
│   │   ├── components/
│   │   ├── pages/
│   │   ├── hooks/
│   │   ├── services/                 # API client (axios/fetch to localhost:8080)
│   │   └── types/
│   └── resources/
│       └── icon.png                  # App icon
├── electron/                         # Can be removed — merged into frontend/electron/
├── scripts/
│   ├── build-backend.sh              # mvn package → copy JAR
│   ├── bundle-jre.sh                 # Download & extract JRE 21 for bundling
│   └── build-all.sh                  # Orchestrates full build
└── package.json                      # Root workspace (optional monorepo orchestrator)
```

> **Note:** The `electron/` root directory is currently empty. We consolidate Electron code under `frontend/electron/` since the main process and renderer share dependencies. Alternatively, the root `electron/` dir can hold a standalone Electron project — the plan uses the `frontend/` approach for tighter Vite integration.

---

## 4. Implementation Steps

### Phase 1: Project Scaffolding

**Step 1.1 — Initialize frontend project**

```bash
cd frontend
npm create vite@latest . -- --template react-ts
npm install
```

Produces: `package.json`, `vite.config.ts`, `tsconfig.json`, `index.html`, `src/`.

**Step 1.2 — Install Electron + build dependencies**

```bash
npm install electron electron-builder vite-electron-plugin electron-updater
npm install -D concurrently wait-on @types/node
```

**Step 1.3 — Install UI dependencies**

```bash
npm install react-router-dom axios lucide-react
npm install -D tailwindcss @tailwindcss/vite
```

**Step 1.4 — Configure Vite for Electron**

`vite.config.ts`:
```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import electron from 'vite-electron-plugin'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    electron({
      entry: 'electron/main.ts',
    }),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
```

---

### Phase 2: Electron Main Process

**Step 2.1 — Create main process**

`frontend/electron/main.ts`:
```ts
import { app, BrowserWindow } from 'electron'
import path from 'path'
import { startBackend, stopBackend } from './ipc-handlers'

let mainWindow: BrowserWindow | null = null

async function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
    icon: path.join(__dirname, '../resources/icon.png'),
    title: 'KnowVault',
  })

  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL)
    mainWindow.webContents.openDevTools()
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }

  mainWindow.on('closed', () => { mainWindow = null })
}

app.whenReady().then(async () => {
  await startBackend()
  await createWindow()
})

app.on('window-all-closed', async () => {
  await stopBackend()
  if (process.platform !== 'darwin') app.quit()
})

app.on('activate', () => {
  if (mainWindow === null) createWindow()
})
```

**Step 2.2 — Backend lifecycle manager**

`frontend/electron/ipc-handlers.ts`:
```ts
import { spawn, ChildProcess } from 'child_process'
import path from 'path'
import { app, dialog } from 'electron'
import net from 'net'

let backendProcess: ChildProcess | null = null
const BACKEND_PORT = 8080
const STARTUP_TIMEOUT = 30_000

function getJarPath(): string {
  if (process.env.VITE_DEV_SERVER_URL) {
    // Dev: use Maven-built JAR
    return path.join(__dirname, '../../backend/target/knowvault-backend-0.0.1-SNAPSHOT.jar')
  }
  // Production: bundled JAR in resources
  return path.join(process.resourcesPath, 'knowvault-backend.jar')
}

function getJavaPath(): string {
  if (process.env.VITE_DEV_SERVER_URL) {
    return 'java' // Use system Java in dev
  }
  // Production: bundled JRE
  return path.join(process.resourcesPath, 'jre', 'bin', 'java')
}

export async function startBackend(): Promise<void> {
  const jarPath = getJarPath()
  const javaPath = getJavaPath()

  return new Promise((resolve, reject) => {
    backendProcess = spawn(javaPath, ['-jar', jarPath], {
      cwd: path.dirname(jarPath),
      stdio: ['pipe', 'pipe', 'pipe'],
      env: { ...process.env, SERVER_PORT: String(BACKEND_PORT) },
    })

    backendProcess.stdout?.on('data', (data: Buffer) => {
      const line = data.toString()
      console.log(`[backend] ${line}`)
      if (line.includes('Started KnowVaultApplication')) {
        resolve()
      }
    })

    backendProcess.stderr?.on('data', (data: Buffer) => {
      console.error(`[backend] ${data}`)
    })

    backendProcess.on('error', (err) => {
      dialog.showErrorBox('Backend Error', `Failed to start backend: ${err.message}`)
      reject(err)
    })

    backendProcess.on('exit', (code) => {
      if (code !== 0 && code !== null) {
        dialog.showErrorBox('Backend Crashed', `Backend exited with code ${code}`)
      }
      backendProcess = null
    })

    // Timeout safety
    setTimeout(() => reject(new Error('Backend startup timeout')), STARTUP_TIMEOUT)
  })
}

export async function stopBackend(): Promise<void> {
  if (backendProcess) {
    backendProcess.kill('SIGTERM')
    // Give it 5s to shut down gracefully
    await new Promise((resolve) => setTimeout(resolve, 5000))
    if (backendProcess) backendProcess.kill('SIGKILL')
    backendProcess = null
  }
}
```

**Step 2.3 — Preload script**

`frontend/electron/preload.ts`:
```ts
import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('electronAPI', {
  getAppVersion: () => ipcRenderer.invoke('get-app-version'),
  onBackendLog: (callback: (log: string) => void) =>
    ipcRenderer.on('backend-log', (_event, log) => callback(log)),
})
```

---

### Phase 3: Backend Packaging

**Step 3.1 — Maven build for production**

`scripts/build-backend.sh`:
```bash
#!/bin/bash
set -e
cd backend
mvn clean package -DskipTests -Pproduction
cp target/knowvault-backend-0.0.1-SNAPSHOT.jar ../frontend/resources/knowvault-backend.jar
echo "Backend JAR built and copied to frontend/resources/"
```

**Step 3.2 — Configure Spring Boot for production**

Add a production profile to `backend/src/main/resources/application-production.yml`:
```yaml
server:
  port: ${SERVER_PORT:8080}
spring:
  datasource:
    url: jdbc:sqlite:${KNOWVAULT_DATA_DIR:./data}/knowvault.db
  jpa:
    hibernate:
      ddl-auto: none
```

This allows the Electron app to override the data directory via environment variable.

**Step 3.3 — JRE bundling script**

`scripts/bundle-jre.sh`:
```bash
#!/bin/bash
# Downloads a minimal JRE 21 for the target platform
# Uses Eclipse Adoptium (Temurin) releases
set -e

PLATFORM=${1:-linux-x64}  # linux-x64, windows-x64, macos-x64, macos-aarch64
JRE_VERSION="21.0.4"
JRE_DIR="frontend/resources/jre"

if [ -d "$JRE_DIR" ]; then
  echo "JRE already bundled at $JRE_DIR"
  exit 0
fi

echo "Downloading JRE 21 for $PLATFORM..."
# Download from Adoptium and extract
# (URL pattern: https://api.adoptium.net/v3/binary/latest/21/ga/$PLATFORM/jre/hotspot/normal/eclipse)
mkdir -p "$JRE_DIR"
# ... download and extract commands ...
echo "JRE bundled at $JRE_DIR"
```

---

### Phase 4: Electron Builder Configuration

**Step 4.1 — Add electron-builder config to `frontend/package.json`**

```json
{
  "name": "knowvault",
  "version": "0.1.0",
  "main": "dist-electron/main.js",
  "scripts": {
    "dev": "concurrently \"vite\" \"cd ../backend && mvn spring-boot:run\"",
    "dev:frontend": "vite",
    "dev:backend": "cd ../backend && mvn spring-boot:run",
    "build": "tsc && vite build && electron-builder",
    "build:backend": "cd ../backend && mvn clean package -DskipTests",
    "build:all": "npm run build:backend && npm run build",
    "preview": "vite preview"
  },
  "build": {
    "appId": "com.knowvault.app",
    "productName": "KnowVault",
    "directories": {
      "output": "release"
    },
    "files": [
      "dist/**/*",
      "dist-electron/**/*",
      "resources/**/*"
    ],
    "extraResources": [
      {
        "from": "resources/knowvault-backend.jar",
        "to": "knowvault-backend.jar"
      },
      {
        "from": "resources/jre",
        "to": "jre"
      },
      {
        "from": "resources/icon.png",
        "to": "icon.png"
      }
    ],
    "win": {
      "target": [
        {
          "target": "nsis",
          "arch": ["x64"]
        }
      ],
      "icon": "resources/icon.png"
    },
    "nsis": {
      "oneClick": false,
      "perMachine": false,
      "allowToChangeInstallationDirectory": true,
      "createDesktopShortcut": true,
      "createStartMenuShortcut": true,
      "shortcutName": "KnowVault",
      "installerIcon": "resources/icon.ico",
      "uninstallerIcon": "resources/icon.ico"
    },
    "mac": {
      "target": [
        {
          "target": "dmg",
          "arch": ["x64", "arm64"]
        }
      ],
      "icon": "resources/icon.icns"
    },
    "linux": {
      "target": ["AppImage", "deb"],
      "icon": "resources/icon.png",
      "category": "Office"
    }
  }
}
```

---

### Phase 5: Development Workflow

**Step 5.1 — Dev mode scripts**

| Command | What it does |
|---|---|
| `npm run dev` | Starts Vite dev server (port 5173) + Spring Boot (port 8080) concurrently |
| `npm run dev:frontend` | Vite only — for UI work when backend is already running |
| `npm run dev:backend` | Spring Boot only — for backend work |

The Vite proxy (`/api` → `localhost:8080`) means the frontend can call `/api/content` and it forwards to the backend seamlessly.

**Step 5.2 — Dev flow**

```
Terminal 1: cd backend && mvn spring-boot:run     (backend on :8080)
Terminal 2: cd frontend && npm run dev:frontend    (Vite on :5173, proxy /api → :8080)
```

Or use `npm run dev` from `frontend/` which runs both via `concurrently`.

---

### Phase 6: Production Build Pipeline

**Step 6.1 — Full build sequence**

```bash
# From frontend/ directory

# 1. Build the backend JAR
npm run build:backend

# 2. Copy JAR to resources
cp ../backend/target/knowvault-backend-0.0.1-SNAPSHOT.jar resources/knowvault-backend.jar

# 3. Bundle JRE (first time only, per platform)
bash ../scripts/bundle-jre.sh linux-x64

# 4. Build React + Electron + Package
npm run build
```

**Step 6.2 — Output artifacts**

| Platform | Output | Location |
|---|---|---|
| Windows | `KnowVault Setup 0.1.0.exe` | `frontend/release/` |
| macOS | `KnowVault-0.1.0.dmg` | `frontend/release/` |
| Linux | `KnowVault-0.1.0.AppImage` | `frontend/release/` |

---

### Phase 7: Data Directory Strategy

The Spring Boot backend uses a relative path (`./data/knowvault.db`). In production, we need an absolute path in the user's home directory.

**Strategy:**

| Mode | Data directory |
|---|---|
| Development | `backend/data/` (relative, current behavior) |
| Production | `~/.knowvault/data/` (user home, survives updates) |

The Electron main process sets `KNOWVAULT_DATA_DIR` environment variable when spawning the backend:
```ts
const dataDir = path.join(app.getPath('home'), '.knowvault', 'data')
fs.mkdirSync(dataDir, { recursive: true })

backendProcess = spawn(javaPath, ['-jar', jarPath], {
  env: { ...process.env, KNOWVAULT_DATA_DIR: dataDir },
})
```

The production `application-production.yml` reads this:
```yaml
spring:
  datasource:
    url: jdbc:sqlite:${KNOWVAULT_DATA_DIR:./data}/knowvault.db
```

---

### Phase 8: Error Handling & Resilience

| Scenario | Handling |
|---|---|
| JAR not found | Show error dialog: "Backend files missing. Please reinstall." |
| Java/JRE not found | Show error dialog with download link (dev mode) or bundled JRE (prod) |
| Port 8080 in use | Try next port (8081, 8082...) or show error |
| Backend crash | Show notification, offer restart button |
| Backend startup timeout | Show error dialog after 30s |
| Frontend can't connect | Retry with exponential backoff, show loading spinner |

---

### Phase 9: Future Enhancements (Not in MVP)

| Feature | Description |
|---|---|
| Auto-update | `electron-updater` with GitHub Releases or custom update server |
| System tray | Minimize to tray, quick search from tray |
| Deep links | `knowvault://` protocol for sharing/bookmarking |
| Code signing | Windows Authenticode, macOS notarization |
| Crash reporting | Sentry or custom crash reporter |
| Splash screen | Show splash while backend starts |
| Offline detection | Handle network-dependent features gracefully |

---

## 5. Dependencies Summary

### New npm packages (frontend)

| Package | Purpose |
|---|---|
| `electron` | Desktop shell |
| `electron-builder` | Packaging & distribution |
| `vite-electron-plugin` | Vite ↔ Electron integration |
| `electron-updater` | Auto-update (future) |
| `react` / `react-dom` | UI framework |
| `react-router-dom` | Client-side routing |
| `axios` | HTTP client for API calls |
| `tailwindcss` | Utility CSS |
| `lucide-react` | Icons |
| `concurrently` | Run multiple dev commands |
| `wait-on` | Wait for backend before opening Electron |

### Existing (backend, no changes needed)

| Package | Purpose |
|---|---|
| `spring-boot-maven-plugin` | Produces executable JAR |
| `sqlite-jdbc` | Database driver |
| All existing deps | No changes required |

---

## 6. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| JRE bundle size (~40MB) | Medium | Low | Use `jlink` to create minimal JRE (~25MB) |
| Backend startup delay (5-10s) | High | Medium | Show splash screen with progress indicator |
| Port conflicts | Low | Medium | Dynamic port discovery, or use Unix socket |
| macOS code signing cost | Medium | Low | Skip signing for dev; $99/year for distribution |
| Platform-specific bugs | Medium | Medium | Test on CI for all 3 platforms |
| Electron security (contextIsolation) | Low | High | Already using contextIsolation + preload |

---

## 7. Testing Strategy

| Test type | What to test | Tool |
|---|---|---|
| Unit | Backend lifecycle (start/stop/restart) | Jest/Vitest |
| Integration | Electron → Backend → API round trip | Playwright (Electron mode) |
| E2E | Full user flow: launch → import → search | Playwright (Electron mode) |
| Smoke | App launches, backend starts, UI loads | Post-build script |
| Platform | Install/uninstall on Win/Mac/Linux | CI matrix (GitHub Actions) |

---

## 8. File Checklist

### Files to create

- [x] `frontend/package.json`
- [x] `frontend/vite.config.ts`
- [x] `frontend/tsconfig.json`
- [x] `frontend/tsconfig.node.json`
- [x] `frontend/index.html`
- [x] `frontend/tailwind.config.ts`
- [x] `frontend/electron/main.ts`
- [x] `frontend/electron/preload.ts`
- [x] `frontend/electron/ipc-handlers.ts`
- [x] `frontend/src/main.tsx`
- [x] `frontend/src/App.tsx`
- [x] `frontend/src/index.css`
- [x] `frontend/src/vite-env.d.ts`
- [x] `frontend/resources/icon.png`
- [x] `scripts/build-backend.sh`
- [x] `scripts/bundle-jre.sh`
- [x] `scripts/build-all.sh`

### Files to modify

- [x] `backend/src/main/resources/application.yml` — add production profile support
- [x] `backend/src/main/resources/application-production.yml` — production config
- [x] `.gitignore` — add `frontend/node_modules/`, `frontend/dist/`, `frontend/release/`

### Files to delete

- [x] `electron/.keep` — no longer needed (Electron code lives in `frontend/electron/`)

---

## 9. Estimated Effort

| Phase | Effort | Dependencies |
|---|---|---|
| Phase 1: Scaffolding | 1-2 hours | None |
| Phase 2: Main process | 2-3 hours | Phase 1 |
| Phase 3: Backend packaging | 1-2 hours | Backend JAR works |
| Phase 4: electron-builder | 2-3 hours | Phases 1-3 |
| Phase 5: Dev workflow | 1 hour | Phases 1-2 |
| Phase 6: Production build | 2-3 hours | Phases 1-5 |
| Phase 7: Data directory | 1 hour | Phase 2 |
| Phase 8: Error handling | 2-3 hours | Phase 2 |
| **Total** | **12-18 hours** | |

---

*End of Plan*
