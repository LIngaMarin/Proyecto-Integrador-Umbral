import { test, expect } from '@playwright/test';
import { createHmac } from 'node:crypto';

/** Tokens efímeros de fixtures H2; no se guarda ninguna credencial real. */
function token(userId) {
  const codificar = objeto => Buffer.from(JSON.stringify(objeto)).toString('base64url');
  const mensaje = `${codificar({ alg: 'HS256', typ: 'JWT' })}.${codificar({ user_id: String(userId), token_type: 'access', sub: String(userId), exp: Math.floor(Date.now() / 1000) + 600 })}`;
  return `${mensaje}.${createHmac('sha256', process.env.UMBRAL_WEB_TEST_SECRET).update(mensaje).digest('base64url')}`;
}

async function buscar(page, tema = 'Algebra', dificultad = 'FACIL') {
  await page.goto('/#practicas');
  await page.getByLabel('Tema', { exact: true }).fill(tema);
  await page.getByLabel('Dificultad').selectOption(dificultad);
  await page.getByRole('button', { name: 'Buscar preguntas' }).click();
}

/** Verificación real de Bootstrap, navegación y ausencia de desbordamiento horizontal. */
test('inicio responsive, menú hamburguesa y navegación sin errores JS', async ({ page }) => {
  const errores = [];
  page.on('pageerror', error => errores.push(error.message));
  page.on('console', mensaje => { if (mensaje.type() === 'error') errores.push(mensaje.text()); });
  for (const ancho of [375, 768, 1440]) {
    await page.setViewportSize({ width: ancho, height: 900 });
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Tu próximo logro');
    expect(await page.locator('.navbar').evaluate(elemento => getComputedStyle(elemento).display)).toBe('flex');
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    if (ancho < 992) {
      await page.getByRole('button', { name: 'Abrir menú' }).click();
      await expect(page.locator('#menu')).toHaveClass(/show/);
    }
    await page.locator('nav').getByRole('link', { name: 'Mis dudas' }).click();
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tus dudas también importan');
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  }
  expect(errores).toEqual([]);
});

test('catálogo real: filtros, pregunta aprobada y estado vacío', async ({ page }) => {
  await buscar(page);
  await expect(page.getByRole('heading', { name: 'DEMO: Resolver 2x + 6 = 14' })).toBeVisible();
  await expect(page.getByText('Pregunta pendiente', { exact: false })).toHaveCount(0);
  await buscar(page, 'Algebra', 'INTERMEDIO');
  await expect(page.getByRole('heading', { name: 'DEMO: Resolver 3x + 9 = 21' })).toBeVisible();
  await buscar(page, 'Tema inexistente');
  await expect(page.getByText('No hay preguntas aprobadas', { exact: false })).toBeVisible();
});

test('validaciones bloquean filtros vacíos y envío protegido sin token', async ({ page }) => {
  let consultas = 0;
  page.on('request', request => { if (request.url().includes('/api/')) consultas++; });
  await page.goto('/#practicas');
  await page.getByRole('button', { name: 'Buscar preguntas' }).click();
  expect(consultas).toBe(0);
  await page.getByLabel('Tema', { exact: true }).fill('   ');
  await page.getByRole('button', { name: 'Buscar preguntas' }).click();
  expect(consultas).toBe(0);
  await buscar(page);
  await page.getByRole('radio').nth(1).check();
  await page.getByRole('button', { name: 'Enviar respuesta' }).click();
  await expect(page.getByRole('alert')).toContainText('token de alumno válido');
});

/** Recorrido con HTTP real: JWT, práctica, resultados, duda, respuesta docente y resolución. */
test('flujo académico completo contra Spring Boot H2', async ({ page, request }) => {
  await page.goto('/#sesion');
  await page.getByLabel('Token de alumno', { exact: true }).fill(token(2));
  await page.getByRole('button', { name: 'Usar token' }).click();
  await expect(page.getByRole('status')).toContainText('Token guardado');
  await page.locator('nav').getByRole('link', { name: 'Prácticas', exact: true }).click();
  await page.getByLabel('Tema', { exact: true }).fill('Algebra');
  await page.getByRole('button', { name: 'Buscar preguntas' }).click();
  await page.getByRole('radio').nth(1).check();
  await page.getByRole('button', { name: 'Enviar respuesta' }).click();
  await expect(page.getByRole('status')).toContainText('¡Respuesta correcta!');
  await page.getByRole('button', { name: 'Consultar solucionario' }).click();
  await expect(page.getByRole('alert')).toContainText('No hay un registro o solución');
  await page.locator('nav').getByRole('link', { name: 'Resultados', exact: true }).click();
  await expect(page.getByText('Tu respuesta: 4')).toBeVisible();
  await page.locator('nav').getByRole('link', { name: 'Mis dudas' }).click();
  await page.getByLabel('ID del docente').fill('1');
  await page.getByLabel('ID de sección').fill('1');
  await page.getByLabel('Tema', { exact: true }).fill('Algebra');
  await page.getByLabel('¿Qué necesitas comprender?').fill('Quiero comprender el despeje.');
  await page.getByRole('button', { name: 'Enviar duda', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('registrada');
  await page.getByRole('button', { name: 'Consultar dudas' }).click();
  await expect(page.getByText('Quiero comprender el despeje.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Marcar como resuelta' }).click();
  await expect(page.getByRole('alert')).toContainText('aún no tiene respuesta');
  // Prepara la respuesta con un docente autorizado, usando exclusivamente endpoints existentes.
  const headers = { Authorization: `Bearer ${token(1)}` };
  const gruposRespuesta = await request.get('http://127.0.0.1:8080/api/docente/dudas/grupos', { headers });
  expect(gruposRespuesta.ok()).toBeTruthy();
  const grupos = await gruposRespuesta.json();
  const respuesta = await request.post(`http://127.0.0.1:8080/api/docente/dudas/grupos/${grupos[0].id}/respuestas`, { headers, data: { texto: 'Resta seis y divide entre dos.' } });
  expect(respuesta.status()).toBe(201);
  await page.getByRole('button', { name: 'Ver respuesta' }).click();
  await expect(page.getByText('Resta seis y divide entre dos.', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Marcar como resuelta' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Duda resuelta.' })).toBeVisible();
  await page.getByLabel('Estado', { exact: true }).selectOption('RESUELTA');
  await page.getByRole('button', { name: 'Consultar dudas' }).click();
  await expect(page.getByText('RESUELTA', { exact: true })).toBeVisible();
  await page.reload();
  await page.getByRole('button', { name: 'Consultar dudas' }).click();
  await expect(page.getByRole('alert')).toContainText('token de alumno válido');
});

/** Fallos controlados: estas respuestas se simulan solo en pruebas, nunca en la web. */
for (const estado of [400, 401, 403, 404, 409, 500]) {
  test(`manejo HTTP ${estado} sin dejar el botón bloqueado`, async ({ page }) => {
    await page.route('**/api/practicas/preguntas?**', route => route.fulfill({ status: estado, body: '{}' }));
    await buscar(page);
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Buscar preguntas' })).toBeEnabled();
  });
}

test('indicador de carga y error de red', async ({ page }) => {
  let liberar;
  const puerta = new Promise(resolve => { liberar = resolve; });
  await page.route('**/api/practicas/preguntas?**', async route => { await puerta; await route.abort(); });
  await buscar(page);
  await expect(page.getByRole('button', { name: 'Procesando…' })).toBeDisabled();
  liberar();
  await expect(page.getByRole('alert')).toContainText('No se pudo conectar');
});
