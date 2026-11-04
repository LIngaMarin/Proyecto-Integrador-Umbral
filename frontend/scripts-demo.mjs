// Arranque local de demostración: la clave y los tokens existen solo en memoria del proceso.
import { randomBytes, createHmac } from 'node:crypto';
import { spawn } from 'node:child_process';
import { resolve } from 'node:path';

const secret = randomBytes(48).toString('base64url');
const java = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin/java.exe') : 'java';
const jar = resolve(import.meta.dirname, '../backend/target/umbral-backend-0.0.1-SNAPSHOT.jar');
const child = spawn(java, ['-jar', jar, '--spring.profiles.active=demo'], { stdio: 'inherit', env: { ...process.env, UMBRAL_JWT_SECRET: secret } });
// Cada token vence en dos horas y solo corresponde a los usuarios del perfil demo.
for (const [rol, user] of [['DOCENTE', 1], ['ALUMNO', 2], ['APODERADO', 3]]) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const claims = Buffer.from(JSON.stringify({ user_id: String(user), token_type: 'access', exp: Math.floor(Date.now() / 1000) + 7200 })).toString('base64url');
  const signature = createHmac('sha256', secret).update(`${header}.${claims}`).digest('base64url');
  console.log(`\nACCESO DEMO LOCAL · ${rol} · usuario ${user}\n${header}.${claims}.${signature}\n`);
}
child.on('error', error => { console.error(error.message); process.exitCode = 1; });
child.on('exit', code => { process.exitCode = code || 0; });
process.on('SIGINT', () => child.kill());
process.on('SIGTERM', () => child.kill());
