import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'


export default defineConfig({

  base: '/vmc-query-builder/',
  plugins: [vue()],
  build: {
    outDir: resolve(__dirname, '../docs'),
    emptyOutDir: true
  }
})
