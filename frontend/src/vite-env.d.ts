/// <reference types="vite/client" />

interface ElectronAPI {
  getAppVersion: () => Promise<string>
  onBackendLog: (callback: (log: string) => void) => () => void
  isElectron: boolean
}

interface Window {
  electronAPI?: ElectronAPI
}
