import { defineConfig } from 'vite';

// Mantiene las solicitudes en el mismo origen y el JWT llega al servidor sin habilitar CORS global.
export default defineConfig({ server: { proxy: { '/api': 'http://127.0.0.1:8080' } } });
