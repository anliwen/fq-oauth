import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: Number(process.env.FQ_FRONTEND_PORT || 8081),
    strictPort: true,
    proxy: {
      // backend fq-server runs on 8080 by default
      '/api': { target: process.env.FQ_API || 'http://localhost:8080', changeOrigin: true },
      '/oauth': { target: process.env.FQ_API || 'http://localhost:8080', changeOrigin: true },
      '/.well-known': { target: process.env.FQ_API || 'http://localhost:8080', changeOrigin: true }
    }
  }
})
