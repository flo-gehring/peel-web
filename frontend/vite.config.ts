import { fileURLToPath, URL } from 'node:url'
import { existsSync } from 'node:fs'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  server: {
    host: '0.0.0.0',
    // Windows bind mounts do not reliably forward filesystem events into Docker.
    watch: {
      usePolling: process.env.CHOKIDAR_USEPOLLING === 'true' || existsSync('/.dockerenv'),
      interval: 300,
    },
    proxy: {
      '/api': {
        target: 'http://host.docker.internal:8080',
      },
    },
  },
  preview: {
    host: '0.0.0.0',
  },
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})
