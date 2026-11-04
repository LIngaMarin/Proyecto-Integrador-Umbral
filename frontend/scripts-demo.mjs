// Arranque demo local: usa hashes de los fixtures y una clave de firma efímera que nunca se imprime.
import { randomBytes } from 'node:crypto';
import { spawn } from 'node:child_process';
import { resolve } from 'node:path';
const java=process.env.JAVA_HOME?resolve(process.env.JAVA_HOME,'bin/java.exe'):'java';
const jar=resolve(import.meta.dirname,'../backend/target/umbral-backend-0.0.1-SNAPSHOT.jar');
const port=Number(process.env.UMBRAL_DEMO_PORT||8080);
if(!Number.isInteger(port)||port<1||port>65535)throw new Error('Puerto demo no válido');
console.log('UMBRAL demo: abre http://localhost:5173 e inicia sesión con alumno.demo, docente.demo o apoderado.demo y su contraseña de prueba.');
const child=spawn(java,['-jar',jar,'--spring.profiles.active=demo',`--server.port=${port}`],{stdio:'inherit',windowsHide:true,env:{...process.env,UMBRAL_JWT_SECRET:randomBytes(48).toString('base64url')}});
child.on('error',error=>{console.error(error.message);process.exitCode=1;});
child.on('exit',code=>{process.exitCode=code||0;});
process.on('SIGINT',()=>child.kill());
process.on('SIGTERM',()=>child.kill());
