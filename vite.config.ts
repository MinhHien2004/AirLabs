import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  root: '.',
  publicDir: 'public',
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src/main/java/Task/demo/frontend')
    }
  },
  build: {
    outDir: 'src/main/resources/static',  // Build trực tiếp vào Spring Boot static folder
    emptyOutDir: true,  // Xóa folder cũ trước khi build
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
