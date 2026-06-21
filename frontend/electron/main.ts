import { app, BrowserWindow, dialog } from 'electron'
import path from 'path'
import { startBackend, stopBackend } from './ipc-handlers'

let mainWindow: BrowserWindow | null = null

const isDev = !!process.env.VITE_DEV_SERVER_URL

async function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 900,
    minHeight: 600,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
    icon: path.join(__dirname, '../resources/icon.png'),
    title: 'KnowVault',
    show: false,
  })

  mainWindow.once('ready-to-show', () => {
    mainWindow?.show()
  })

  if (isDev) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL!)
    mainWindow.webContents.openDevTools()
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

app.whenReady().then(async () => {
  try {
    if (!isDev) {
      await startBackend()
    }
    await createWindow()
  } catch (err) {
    dialog.showErrorBox(
      'Startup Error',
      `Failed to start KnowVault: ${err instanceof Error ? err.message : String(err)}`
    )
    app.quit()
  }
})

app.on('window-all-closed', async () => {
  await stopBackend()
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', async () => {
  if (mainWindow === null) {
    await createWindow()
  }
})

app.on('before-quit', async () => {
  await stopBackend()
})
