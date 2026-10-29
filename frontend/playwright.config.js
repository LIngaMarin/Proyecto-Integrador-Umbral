import { defineConfig } from '@playwright/test';
import { randomBytes } from 'node:crypto';

// Clave efímera exclusiva de las pruebas H2, compartida con la instancia de demo.
process.env.UMBRAL_WEB_TEST_SECRET ||= randomBytes(48).toString('hex');
export default defineConfig({
  testDir: './tests', workers: 1, timeout: 30000,
  use: { baseURL: 'http://127.0.0.1:5173', trace: 'retain-on-failure' },
  webServer: [
    { command: 'npm run dev -- --port 5173 --strictPort', url: 'http://127.0.0.1:5173', reuseExistingServer: false },
    {
      // Se usa el JAR demo existente sin modificar fuentes ni configuración del backend.
      command: '"%JAVA_HOME%/bin/java.exe" -jar ../backend/target/umbral-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=8080',
      url: 'http://127.0.0.1:8080/api/practicas/preguntas?tema=Algebra&dificultad=FACIL',
      env: { UMBRAL_JWT_SECRET: process.env.UMBRAL_WEB_TEST_SECRET }, timeout: 120000, reuseExistingServer: false,
    },
  ],
});
