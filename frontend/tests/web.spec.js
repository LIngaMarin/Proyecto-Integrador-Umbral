import { test, expect } from '@playwright/test';
import { createHmac } from 'node:crypto';

/** Conserva los escenarios remotos y los adapta al acceso por perfil y al catálogo público. */
function token(userId) {
  const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
  const data = encode({alg:'HS256',typ:'JWT'})+'.'+encode({user_id:String(userId),token_type:'access',exp:Math.floor(Date.now()/1000)+600});
  return data+'.'+createHmac('sha256',process.env.UMBRAL_WEB_TEST_SECRET).update(data).digest('base64url');
}
async function publico(page) {
  await page.goto('/');
  await page.getByRole('button',{name:'Explorar prácticas públicas'}).click();
  await expect(page.getByRole('heading',{name:'Prácticas',exact:true})).toBeVisible();
}
async function buscar(page,tema='Algebra',dificultad='FACIL') {
  await publico(page);
  await page.getByLabel('Tema',{exact:true}).fill(tema);
  await page.getByLabel('Dificultad').selectOption(dificultad);
  await page.getByRole('button',{name:'Buscar preguntas'}).click();
}
async function alumno(page) {
  await page.goto('/');
  await page.locator('[name=token]').fill(token(2));
  await page.getByRole('button',{name:'Ingresar al campus'}).click();
  await expect(page.getByRole('heading',{name:'Bienvenido a UMBRAL.'})).toBeVisible();
}
test('catálogo público responsive y navegación sin errores JS',async({page})=>{
  const errores=[];
  page.on('pageerror',e=>errores.push(e.message));
  for(const width of [375,768,1440]) {
    await page.setViewportSize({width,height:900});
    await publico(page);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    await page.getByRole('link',{name:'Ingresar',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Bienvenido de nuevo.'})).toBeVisible();
  }
  expect(errores).toEqual([]);
});
test('catálogo real con filtros y estado vacío',async({page})=>{
  await buscar(page);
  await expect(page.getByRole('heading',{name:'Resolver 2x + 6 = 14'})).toBeVisible();
  await buscar(page,'Algebra','INTERMEDIO');
  await expect(page.getByRole('heading',{name:'Resolver 3x + 9 = 21'})).toBeVisible();
  await buscar(page,'Tema inexistente');
  await expect(page.getByText('No hay preguntas publicadas para este tema y dificultad.')).toBeVisible();
});
test('validaciones bloquean filtros vacíos y envío sin acceso',async({page})=>{
  let consultas=0;
  page.on('request',r=>{if(r.url().includes('/api/'))consultas++;});
  await publico(page);
  await page.getByRole('button',{name:'Buscar preguntas'}).click();
  expect(consultas).toBe(0);
  await page.getByLabel('Tema',{exact:true}).fill('   ');
  await page.getByRole('button',{name:'Buscar preguntas'}).click();
  expect(consultas).toBe(0);
  await buscar(page);
  await page.getByRole('radio').nth(1).check();
  await page.getByRole('button',{name:'Enviar respuesta'}).click();
  await expect(page.getByRole('alert')).toContainText('Ingresa con tu acceso');
});
test('resultados conservan solo intentos confirmados de la sesión',async({page})=>{
  await alumno(page);
  await page.getByRole('link',{name:'Prácticas',exact:true}).click();
  await page.getByLabel('Tema',{exact:true}).fill('Algebra');
  await page.getByRole('button',{name:'Buscar preguntas'}).click();
  await page.getByRole('radio').nth(1).check();
  await page.getByRole('button',{name:'Enviar respuesta'}).click();
  await expect(page.getByText('Respuesta correcta. ¡Sigue avanzando!')).toBeVisible();
  await page.getByRole('link',{name:'Resultados',exact:true}).click();
  await expect(page.getByText('Tu respuesta: 4')).toBeVisible();
  await page.getByRole('button',{name:'Cerrar sesión'}).click();
  await alumno(page);
  await page.getByRole('link',{name:'Resultados',exact:true}).click();
  await expect(page.getByText('Todavía no has enviado respuestas en esta sesión.')).toBeVisible();
});
// Los fallos forzados se limitan a pruebas; no hay respuestas simuladas en producción.
for(const status of [400,401,403,404,409,500]) {
  test('manejo HTTP '+status+' recupera el botón',async({page})=>{
    await page.route('**/api/practicas/preguntas?**',r=>r.fulfill({status,body:'{}'}));
    await buscar(page);
    await expect(page.getByRole('alert')).toBeVisible();
    if(status===401)await expect(page.getByRole('button',{name:'Ingresar al campus'})).toBeEnabled();
    else await expect(page.getByRole('button',{name:'Buscar preguntas'})).toBeEnabled();
  });
}
test('error de red recupera el control y explica el fallo',async({page})=>{
  await page.route('**/api/practicas/preguntas?**',r=>r.abort());
  await buscar(page);
  await expect(page.getByRole('alert')).toContainText('No se pudo conectar');
  await expect(page.getByRole('button',{name:'Buscar preguntas'})).toBeEnabled();
});
