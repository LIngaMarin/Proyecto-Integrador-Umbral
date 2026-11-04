import { defineConfig } from 'vite';

// Mantiene las solicitudes en el mismo origen y el JWT llega al servidor sin habilitar CORS global.
const proxy = { '/api': 'http://127.0.0.1:8080' };
export default defineConfig({ server: { proxy }, preview: { proxy } });
