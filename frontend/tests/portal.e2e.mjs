// Verificación integral con Edge: backend demo aislado, JWT efímero y operaciones reales.
import { chromium, expect } from '@playwright/test';
import { spawn } from 'node:child_process';
import { randomBytes, createHmac } from 'node:crypto';
import { resolve } from 'node:path';
import { mkdir, writeFile, readFile } from 'node:fs/promises';

const root = resolve(import.meta.dirname, '..');
const secret = randomBytes(48).toString('base64url');
const testPassword = randomBytes(24).toString('base64url');
const processes = [];
const errors = [];
let browser;
let backendLog = '';
function token(user) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const payload = Buffer.from(JSON.stringify({ user_id: String(user), token_type: 'access', exp: Math.floor(Date.now()/1000)+600 })).toString('base64url');
  return `${header}.${payload}.${createHmac('sha256', secret).update(`${header}.${payload}`).digest('base64url')}`;
}
async function ready(url) {
  for (let i=0; i<160; i++) {
    try { if ((await fetch(url)).ok) return; } catch {}
    await new Promise(done => setTimeout(done, 250));
  }
  throw new Error(`No inició ${url}\n${backendLog.slice(-3000)}`);
}
async function enter(page, user) {
  await page.goto('http://127.0.0.1:5178/#acceso');
  await page.locator('[name=usuario]').fill(({1:'docente.demo',2:'alumno.demo',3:'apoderado.demo'})[user]);
  await page.locator('[name=password]').fill(testPassword);
  await page.getByRole('button', { name: 'Iniciar sesión', exact:true }).click();
  await expect(page.getByRole('heading', { name: 'Bienvenido a UMBRAL.' })).toBeVisible();
}
async function link(page, label, heading) {
  await page.getByRole('link', { name: label, exact: true }).click();
  await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
}
/** PDF de prueba real; el servidor debe extraer exactamente este contenido antes de guardarlo. */
function bulletinPdf(){
  const lines=['Tema: Geometria','Dificultad: FACIL','Pregunta: Cuanto es 2 + 4?','A) 4','B) 6','C) 8','Correcta: B'];
  const content='BT /F1 12 Tf 50 760 Td 18 TL '+lines.map((line,i)=>(i?'T* ':'')+'('+line.replace(/[\\()]/g,'\\$&')+') Tj').join('\n')+' ET';
  const objects=['<< /Type /Catalog /Pages 2 0 R >>','<< /Type /Pages /Kids [3 0 R] /Count 1 >>','<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>','<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',`<< /Length ${Buffer.byteLength(content)} >>\nstream\n${content}\nendstream`];
  let pdf='%PDF-1.4\n';const offsets=[0];
  objects.forEach((o,i)=>{offsets.push(Buffer.byteLength(pdf));pdf+=`${i+1} 0 obj\n${o}\nendobj\n`;});
  const xref=Buffer.byteLength(pdf);
  pdf+='xref\n0 6\n0000000000 65535 f \n'+offsets.slice(1).map(o=>String(o).padStart(10,'0')+' 00000 n \n').join('')+`trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF`;
  return Buffer.from(pdf);
}
try {
  const java = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin/java.exe') : 'java';
  // Una ruta alternativa permite verificar otro artefacto sin tocar el servidor del usuario.
  const jar=process.env.UMBRAL_BACKEND_JAR || resolve(root, '../backend/target/umbral-backend-0.0.1-SNAPSHOT.jar');
  const backend = spawn(java, ['-jar', jar, '--spring.profiles.active=demo', '--server.port=18089', '--umbral.resumen.enabled=false', '--umbral.asistencia.intervalo-ms=1000'], { env: { ...process.env, UMBRAL_JWT_SECRET: secret, UMBRAL_DEMO_TEST_PASSWORD: testPassword }, windowsHide: true });
  processes.push(backend);
  backend.stdout.on('data', data => { backendLog += data; });
  backend.stderr.on('data', data => { backendLog += data; });
  backend.on('error', error => errors.push(error.message));
  // Configuración proxy propia de la prueba; el desarrollo habitual conserva el puerto 8080.
  const vite = spawn(process.execPath, [resolve(root, 'node_modules/vite/bin/vite.js'), '--config', resolve(root, 'tests/vite.e2e.js'), '--port', '5178', '--strictPort'], { cwd: root, windowsHide: true });
  processes.push(vite);
  vite.stdout.on('data', () => {}); vite.stderr.on('data', data => errors.push(String(data)));
  await ready('http://127.0.0.1:18089/api/practicas/preguntas?tema=Algebra&dificultad=FACIL');
  await ready('http://127.0.0.1:5178');
  browser = await chromium.launch({ executablePath: process.env.UMBRAL_BROWSER || 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true });
  const teacher = await browser.newPage();
  const student = await browser.newPage();
  const parent = await browser.newPage();
  for (const page of [teacher, student, parent]) page.on('pageerror', error => errors.push(error.message));
  await enter(student, 2);
  await link(student, 'Prácticas', 'Prácticas');
  await student.locator('[name=tema]').fill('Algebra');
  await student.getByRole('button', { name: 'Buscar preguntas' }).click();
  await expect(student.locator('[data-practice]')).toHaveCount(1);
  await student.locator('input[type=radio][value="4"]').check();
  await student.getByRole('button', { name: 'Enviar respuesta' }).click();
  await expect(student.getByText('Respuesta correcta. ¡Sigue avanzando!')).toBeVisible();
  await enter(teacher, 1);
  await link(teacher, 'Banco de preguntas', 'Banco de preguntas');
  const q = teacher.locator('[data-question]').first().locator('..');
  await q.getByRole('button', { name: 'Generar solucionario', exact: true }).click();
  await expect(q.locator('[id^=solution-]')).toContainText('BORRADOR');
  await q.getByRole('button', { name: 'Revisar solucionario', exact: true }).click();
  await q.getByRole('button', { name: 'Aprobar solucionario revisado' }).click();
  await expect(q.getByText('Solucionario aprobado.', { exact: true })).toBeVisible();
  const pending = teacher.locator('[data-question="3"]').locator('..');
  await pending.locator('[name=motivoRechazo]').fill('Revisar la redacción del enunciado.');
  await pending.getByRole('button', { name: 'Rechazar', exact: true }).click();
  await expect(pending.locator('.status')).toHaveText('RECHAZADA');
  await pending.getByRole('button', { name: 'Guardar y aprobar' }).click();
  await expect(pending.locator('.status')).toHaveText('APROBADA');
  await teacher.locator('#bank-search [name=dificultad]').selectOption('INTERMEDIO');
  await teacher.getByRole('button', { name: 'Filtrar', exact: true }).click();
  await expect(teacher.locator('[data-question]')).toHaveCount(1);
  await student.getByRole('button', { name: 'Ver solucionario' }).click();
  await expect(student.locator('[id^=attempt-]')).toContainText('Dividimos');
  await link(student, 'Mis consultas', 'Mis consultas académicas');
  for (const texto of ['¿Cómo despejar la variable?', '¿Por qué se divide entre dos?']) {
    await student.locator('#new-doubt [name=tema]').fill('Álgebra lineal');
    await student.locator('#new-doubt [name=texto]').fill(texto);
    await student.getByRole('button', { name: 'Enviar consulta' }).click();
    await expect(student.getByText('Tu consulta fue registrada.', { exact: true })).toBeVisible();
  }
  await link(teacher, 'Consultas académicas', 'Centro de consultas académicas');
  await expect(teacher.getByText('2 consultas', { exact: true })).toBeVisible();
  await teacher.locator('[data-group-response] textarea').fill('Resta el término independiente y divide por el coeficiente.');
  await teacher.getByRole('button', { name: 'Enviar respuesta al grupo' }).click();
  await expect(teacher.getByText('La respuesta se guardó para las consultas del grupo.')).toBeVisible();
  await student.reload();
  await student.getByRole('button', { name: 'Ver respuesta', exact: true }).first().click();
  await expect(student.getByText('Resta el término independiente y divide por el coeficiente.', { exact: true })).toBeVisible();
  await student.getByRole('button', { name: 'Marcar como resuelta' }).first().click();
  await expect(student.getByText('La consulta quedó resuelta.')).toBeVisible();
  await student.locator('#doubt-state').selectOption('RESUELTA');
  await expect(student.locator('#doubts .question')).toHaveCount(1);
  await student.getByRole('button', { name: 'Ver historial' }).click();
  await expect(student.locator('[id^=detail-]')).toContainText('RESUELTA');
  await link(student,'Resultados','Mis resultados');
  await expect(student.getByText('Tu respuesta: 4')).toBeVisible();
  await student.reload();
  await expect(student.getByText('Tu respuesta: 4')).toBeVisible();
  await student.locator('#result-state').selectOption('incorrecta');
  await expect(student.getByText('Todavía no tienes intentos registrados para este filtro.')).toBeVisible();
  await link(student,'Simulacros','Simulacros y calendario');
  await student.locator('[name=desde]').fill('2026-11-01');
  await student.locator('[name=hasta]').fill('2026-11-30');
  await student.getByRole('button',{name:'Consultar calendario'}).click();
  await expect(student.getByRole('heading',{name:'Simulacro demo'})).toBeVisible();
  await teacher.reload();
  await teacher.locator('#group-search').fill('Sin coincidencias');
  await expect(teacher.getByText('No hay consultas para estos filtros.')).toBeVisible();
  await teacher.locator('#group-search').fill('Álgebra');
  await teacher.locator('#group-state').selectOption('PENDIENTE');
  await teacher.locator('summary').click();
  await teacher.getByRole('button', { name: 'Ver respuestas del grupo' }).click();
  await expect(teacher.locator('[id^=group-history-]')).toContainText('Resta el término');
  await teacher.getByRole('button', { name: 'Ver historial', exact: true }).click();
  await expect(teacher.locator('[id^=detail-]')).toContainText('RESPONDIDA');
  await teacher.getByRole('button', { name: 'Marcar como resuelta' }).click();
  await expect(teacher.getByText('La consulta quedó resuelta.')).toBeVisible();
  await link(teacher, 'Resultados', 'Resultados y reportes');
  await teacher.getByRole('button',{name:'Consultar',exact:true}).click();
  await expect(teacher.locator('#weekly-result')).toContainText('Preguntas creadas');
  await teacher.getByRole('button', { name: 'Consultar temas por reforzar' }).click();
  await expect(teacher.locator('#weak-topics')).toContainText('Algebra');
  const downloaded = teacher.waitForEvent('download');
  await teacher.getByRole('button', { name: 'Descargar informe PDF' }).click();
  expect((await downloaded).suggestedFilename()).toBe('rendimiento-seccion-1.pdf');
  // HU41: consulta previa, filtro y descarga desde la sección nueva, manteniendo la descarga anterior.
  await link(teacher,'Reportes académicos','Reportes académicos');
  await expect(teacher.locator('#report-preview')).toContainText('Demo A');
  await teacher.locator('#report-filters [name=simulacro]').selectOption('1');
  await expect(teacher.locator('#report-status')).toHaveText('Resultados actualizados.');
  const reporteDescargado=teacher.waitForEvent('download');
  await teacher.getByRole('button',{name:'Exportar PDF',exact:true}).click();
  const reporte=await reporteDescargado;
  await mkdir(resolve(root,'test-results'),{recursive:true});
  await reporte.saveAs(resolve(root,'test-results/hu41-flujo.pdf'));
  expect((await readFile(resolve(root,'test-results/hu41-flujo.pdf'))).subarray(0,5).toString()).toBe('%PDF-');
  await teacher.screenshot({path:resolve(root,'test-results/hu41-reportes.png'),fullPage:true});
  await teacher.setViewportSize({width:390,height:844});
  expect(await teacher.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await teacher.screenshot({path:resolve(root,'test-results/hu41-reportes-movil.png'),fullPage:true});
  await teacher.setViewportSize({width:1280,height:900});
  await link(teacher,'Banco de preguntas','Banco de preguntas');
  await teacher.locator('[data-variant]').first().click();
  await expect(teacher.getByText('La variante se guardó para revisión.')).toBeVisible();
  await teacher.locator('#upload-bulletin input[type=file]').setInputFiles({name:'boletin.pdf',mimeType:'application/pdf',buffer:bulletinPdf()});
  await teacher.getByRole('button',{name:'Importar preguntas'}).click();
  await expect(teacher.getByText('1 preguntas importadas para revisión.')).toBeVisible();
  await expect(teacher.getByRole('heading',{name:'Geometria',exact:true})).toBeVisible();
  await link(teacher, 'Clases y asistencia', 'Clases y asistencia');
  await teacher.locator('[name=titulo]').fill('Repaso de ecuaciones');
  const tomorrow = new Date(Date.now()+86400000).toISOString().slice(0,16);
  await teacher.locator('[name=fin]').fill(tomorrow);
  await teacher.getByRole('button', { name: 'Programar clase' }).click();
  await expect(teacher.getByText('La clase fue programada.')).toBeVisible();
  await teacher.getByRole('button', { name: 'Ver asistencia' }).click();
  await teacher.getByRole('button', { name: 'Registrar presente' }).click();
  await expect(teacher.getByText('PRESENTE', { exact: true })).toBeVisible();
  await teacher.getByRole('button',{name:'Cerrar asistencia',exact:true}).click();
  await expect(teacher.getByRole('alert')).toContainText('La clase todavía no ha finalizado');
  await enter(parent, 3);
  await link(parent, 'Calendario', 'Calendario académico');
  await parent.locator('[name=desde]').fill('2026-11-01');
  await parent.locator('[name=hasta]').fill('2026-11-30');
  await parent.getByRole('button', { name: 'Consultar calendario' }).click();
  await expect(parent.getByRole('heading', { name: 'Simulacro demo' })).toBeVisible();
  await link(parent, 'Notificaciones', 'Notificaciones académicas');
  await expect(parent.getByText('No tienes notificaciones de este alumno.')).toBeVisible();
  // Crea una clase real de duración breve para comprobar también el aviso en el navegador.
  const finLima = new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Lima', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(new Date(Date.now()+3000)).replace(' ', 'T');
  const nueva = await fetch('http://127.0.0.1:18089/api/docente/clases', { method: 'POST', headers: { Authorization: `Bearer ${token(1)}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ seccionId: 1, titulo: 'Clase de seguimiento académico', fin: finLima }) });
  expect(nueva.status).toBe(201);
  await expect.poll(async () => {
    const r = await fetch('http://127.0.0.1:18089/api/apoderado/alumnos/2/notificaciones', { headers: { Authorization: `Bearer ${token(3)}` } });
    return (await r.json()).length;
  }, { timeout: 15000 }).toBe(1);
  await parent.reload();
  await expect(parent.getByText('No se registró asistencia a la clase: Clase de seguimiento académico')).toBeVisible();
  await link(parent, 'Mi cuenta', 'Mi cuenta');
  await expect(parent.getByRole('heading', { name: 'apoderado@example.invalid' })).toBeVisible();
  await link(parent, 'Notificaciones', 'Notificaciones académicas');
  await parent.setViewportSize({ width: 390, height: 844 });
  expect(await parent.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
  await mkdir(resolve(root, 'test-results'), { recursive: true });
  await parent.screenshot({ path: resolve(root, 'test-results/apoderado-movil.png'), fullPage: true });
  await teacher.screenshot({ path: resolve(root, 'test-results/docente.png'), fullPage: true });
  await student.getByRole('button', { name: 'Cerrar sesión' }).click();
  expect(await student.evaluate(() => sessionStorage.getItem('umbral.access'))).toBeNull();
  expect(errors).toEqual([]);
  console.log('E2E correcto: JWT de tres roles, práctica, solucionario, agrupación, propagación, resolución, historial, reporte PDF, asistencia, calendario, notificaciones, móvil y cierre de sesión. Sin errores JavaScript.');
} catch (error) {
  await mkdir(resolve(root, 'test-results'), { recursive: true });
  await writeFile(resolve(root, 'test-results/backend.log'), backendLog);
  throw error;
} finally {
  await browser?.close();
  for (const child of processes) child.kill();
}
