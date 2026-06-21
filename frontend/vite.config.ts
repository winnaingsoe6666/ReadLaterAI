import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import electron from 'vite-plugin-electron'
import renderer from 'vite-plugin-electron-renderer'

// Skip Electron in headless/WSL mode to prevent crashes
// Set VITE_ELECTRON=true to enable Electron desktop build
const isHeadless = !process.env.VITE_ELECTRON

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
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 1800000,
        proxyTimeout: 1800000,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            // Preserve Content-Type header for multipart uploads
            const ct = req.headers['content-type'];
            if (ct) {
              proxyReq.setHeader('Content-Type', ct);
            }
          });
        },
      },
    },
  },
})
