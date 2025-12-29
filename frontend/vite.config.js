import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(), 
    tailwindcss()],
  define: {
    // Polyfill Node-style global for browser libraries like sockjs-client
    global: 'window',
  },
})
