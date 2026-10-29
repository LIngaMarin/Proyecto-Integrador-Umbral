import { defineConfig } from 'vite';

// El proxy conserva /api y evita cambiar CORS o la seguridad de Spring Boot.
const proxy = { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true } };
export default defineConfig({ server: { proxy }, preview: { proxy } });
