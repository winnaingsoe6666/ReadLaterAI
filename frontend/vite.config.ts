import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import electron from 'vite-plugin-electron'
import renderer from 'vite-plugin-electron-renderer'

// Skip Electron in headless mode (no DISPLAY) to prevent crashes
const isHeadless = !process.env.DISPLAY

export default defineConfig({
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  plugins: [
    react(),
    tailwindcss(),
    ...(isHeadless
      ? []
      : [
          electron([
            {
              entry: 'electron/main.ts',
            },
          ]),
          renderer(),
        ]),
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
