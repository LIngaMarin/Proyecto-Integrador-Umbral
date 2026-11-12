import { defineConfig } from 'vite';
// Servidor aislado de regresión: no toca la instancia del usuario.
export default defineConfig({server:{host:'127.0.0.1',proxy:{'/api':'http://127.0.0.1:18091'}}});
