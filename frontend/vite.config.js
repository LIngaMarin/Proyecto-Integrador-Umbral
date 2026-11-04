import { defineConfig } from 'vite';

// Mantiene las solicitudes en el mismo origen y el JWT llega al servidor sin habilitar CORS global.
// Permite ejecutar una instancia nueva sin detener otro backend que ya ocupe 8080.
const proxy = { '/api': process.env.UMBRAL_API_TARGET || 'http://127.0.0.1:8080' };
export default defineConfig({ server: { proxy }, preview: { proxy } });
