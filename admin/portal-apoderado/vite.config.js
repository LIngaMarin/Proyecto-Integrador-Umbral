import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// El panel de administración usa el puerto 5173; el portal del apoderado, el 5174.
export default defineConfig({
  plugins: [react()],
  server: { port: 5174, strictPort: true },
  preview: { port: 5174, strictPort: true },
})
