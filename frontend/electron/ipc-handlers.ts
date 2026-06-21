import { spawn, ChildProcess } from 'child_process'
import path from 'path'
import fs from 'fs'
import { app } from 'electron'

let backendProcess: ChildProcess | null = null
const BACKEND_PORT = 8080
const STARTUP_TIMEOUT = 30_000

function getJarPath(): string {
  if (process.env.VITE_DEV_SERVER_URL) {
    return path.join(__dirname, '../../backend/target/knowvault-backend-0.0.1-SNAPSHOT.jar')
  }
  return path.join(process.resourcesPath, 'knowvault-backend.jar')
}

function getJavaPath(): string {
  if (process.env.VITE_DEV_SERVER_URL) {
    return 'java'
  }
  const bundledJre = path.join(process.resourcesPath, 'jre', 'bin', 'java')
  if (fs.existsSync(bundledJre)) {
    return bundledJre
  }
  return 'java'
}

function getDataDir(): string {
  if (process.env.VITE_DEV_SERVER_URL) {
    return path.join(__dirname, '../../backend/data')
  }
  const dataDir = path.join(app.getPath('home'), '.knowvault', 'data')
  fs.mkdirSync(dataDir, { recursive: true })
  return dataDir
}

export async function startBackend(): Promise<void> {
  const jarPath = getJarPath()
  const javaPath = getJavaPath()
  const dataDir = getDataDir()

  if (!fs.existsSync(jarPath)) {
    throw new Error(`Backend JAR not found at: ${jarPath}`)
  }

  return new Promise((resolve, reject) => {
    backendProcess = spawn(javaPath, ['-jar', jarPath], {
      cwd: path.dirname(jarPath),
      stdio: ['pipe', 'pipe', 'pipe'],
      env: {
        ...process.env,
        SERVER_PORT: String(BACKEND_PORT),
        KNOWVAULT_DATA_DIR: dataDir,
      },
    })

    let started = false

    backendProcess.stdout?.on('data', (data: Buffer) => {
      const line = data.toString()
      console.log(`[backend] ${line}`)
      if (!started && line.includes('Started KnowVaultApplication')) {
        started = true
        resolve()
      }
    })

    backendProcess.stderr?.on('data', (data: Buffer) => {
      console.error(`[backend-err] ${data}`)
    })

    backendProcess.on('error', (err) => {
      if (!started) {
        started = true
        reject(new Error(`Failed to start backend: ${err.message}`))
      }
    })

    backendProcess.on('exit', (code) => {
      if (!started) {
        started = true
        reject(new Error(`Backend exited with code ${code} before startup completed`))
      }
      backendProcess = null
    })

    setTimeout(() => {
      if (!started) {
        started = true
        reject(new Error('Backend startup timed out after 30 seconds'))
      }
    }, STARTUP_TIMEOUT)
  })
}

export async function stopBackend(): Promise<void> {
  if (!backendProcess) return

  return new Promise((resolve) => {
    const proc = backendProcess!
    backendProcess = null

    const killTimer = setTimeout(() => {
      proc.kill('SIGKILL')
      resolve()
    }, 5000)

    proc.on('exit', () => {
      clearTimeout(killTimer)
      resolve()
    })

    proc.kill('SIGTERM')
  })
}
