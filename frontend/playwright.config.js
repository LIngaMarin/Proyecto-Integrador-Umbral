import { defineConfig } from '@playwright/test';
import { randomBytes } from 'node:crypto';

// Mantiene los escenarios existentes con puertos aislados y clave exclusiva de H2.
process.env.UMBRAL_WEB_TEST_SECRET ||= randomBytes(48).toString('hex');
export default defineConfig({
  testDir:'./tests',testMatch:'web.spec.js',workers:1,timeout:30000,
  use:{
    baseURL:'http://127.0.0.1:5179',trace:'off',
    // Las trazas podrían contener el JWT introducido: no se guardan ni en fallos.
    launchOptions:{executablePath:process.env.UMBRAL_BROWSER||'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'},
  },
  webServer:[
    {command:'node node_modules/vite/bin/vite.js --config tests/vite.browser.js --port 5179 --strictPort',url:'http://127.0.0.1:5179',reuseExistingServer:false},
    {command:'"%JAVA_HOME%/bin/java.exe" -jar ../backend/target/umbral-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=18091 --umbral.resumen.enabled=false',
     url:'http://127.0.0.1:18091/api/practicas/preguntas?tema=Algebra&dificultad=FACIL',env:{UMBRAL_JWT_SECRET:process.env.UMBRAL_WEB_TEST_SECRET},timeout:60000,reuseExistingServer:false},
  ],
});
