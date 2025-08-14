import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  base: '/vmc-query-builder/',
  plugins: [vue()],
  build: {
    outDir: 'docs'
  }
})
