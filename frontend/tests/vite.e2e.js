import { defineConfig } from 'vite';
// Aísla la prueba del backend que el usuario pudiera tener ejecutándose en 8080.
export default defineConfig({ server: { host: '127.0.0.1', proxy: { '/api': 'http://127.0.0.1:18089' } } });
