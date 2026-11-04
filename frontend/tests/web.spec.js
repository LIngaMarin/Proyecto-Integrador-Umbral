import { test, expect } from '@playwright/test';

/** Conserva los escenarios remotos y los adapta al acceso por perfil y al catálogo público. */
async function publico(page) {
  await page.goto('/');
  if((page.viewportSize()?.width||1440)<600)await page.getByRole('button',{name:'Menú',exact:true}).click();
  await page.getByRole('navigation').getByRole('link',{name:'Prácticas',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Prácticas',exact:true})).toBeVisible();
}
async function buscar(page,tema='Algebra',dificultad='FACIL') {
  await publico(page);
  await page.getByLabel('Tema',{exact:true}).fill(tema);
  await page.getByLabel('Dificultad').selectOption(dificultad);
  await page.getByRole('button',{name:'Buscar preguntas'}).click();
}
async function alumno(page) {
  await page.goto('/#acceso');
  await page.locator('[name=usuario]').fill('alumno.demo');
  await page.locator('[name=password]').fill(process.env.UMBRAL_WEB_TEST_PASSWORD);
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
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
    await expect(page.getByRole('heading',{name:'Qué bueno verte.'})).toBeVisible();
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
test('resultados conservan intentos persistidos después de cerrar sesión',async({page})=>{
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
  await expect(page.getByText('Tu respuesta: 4')).toBeVisible();
});
// Los fallos forzados se limitan a pruebas; no hay respuestas simuladas en producción.
for(const status of [400,401,403,404,409,500]) {
  test('manejo HTTP '+status+' recupera el botón',async({page})=>{
    await page.route('**/api/practicas/preguntas?**',r=>r.fulfill({status,body:'{}'}));
    await buscar(page);
    await expect(page.getByRole('alert')).toBeVisible();
    if(status===401)await expect(page.getByRole('button',{name:'Iniciar sesión',exact:true})).toBeEnabled();
    else await expect(page.getByRole('button',{name:'Buscar preguntas'})).toBeEnabled();
  });
}
test('error de red recupera el control y explica el fallo',async({page})=>{
  await page.route('**/api/practicas/preguntas?**',r=>r.abort());
  await buscar(page);
  await expect(page.getByRole('alert')).toContainText('No se pudo conectar');
  await expect(page.getByRole('button',{name:'Buscar preguntas'})).toBeEnabled();
});

/** Portada real: controles, indicadores, rotación y pausa accesibles por teclado y pantalla táctil. */
test('carrusel avanza retrocede cambia indicadores y permite pausa',async({page})=>{
  await page.goto('/');
  await expect(page.locator('.carousel-item')).toHaveCount(3);
  await expect(page.locator('.carousel-item.active')).toContainText('Tu próximo objetivo');
  await page.getByRole('button',{name:'Diapositiva siguiente'}).click();
  await expect(page.locator('.carousel-item.active')).toContainText('La constancia');
  await page.getByRole('button',{name:'Diapositiva anterior'}).click();
  await expect(page.locator('.carousel-item.active')).toContainText('Tu próximo objetivo');
  await page.getByRole('button',{name:'Ir a diapositiva 3'}).click();
  await expect(page.locator('.carousel-item.active')).toContainText('Pregunta. Comprende.');
  await page.getByRole('button',{name:'Pausar carrusel'}).click();
  await expect(page.getByRole('button',{name:'Reanudar carrusel'})).toHaveAttribute('aria-pressed','true');
});
test('movimiento reducido desactiva rotacion automatica y mantiene controles',async({page})=>{
  await page.emulateMedia({reducedMotion:'reduce'});
  await page.goto('/');
  await expect(page.getByRole('button',{name:'Reanudar carrusel'})).toBeVisible();
  expect(await page.locator('.carousel-item').first().evaluate(e=>getComputedStyle(e).transitionDuration)).toBe('0s');
  await page.getByRole('button',{name:'Diapositiva siguiente'}).click();
  await expect(page.locator('.carousel-item.active')).toContainText('La constancia');
});
test('portada responsive no contiene instrucciones tecnicas ni identidad familiar',async({page})=>{
  for(const width of [375,768,1440]){
    await page.setViewportSize({width,height:900});
    await page.goto('/');
    await expect(page.getByRole('heading',{level:1})).toContainText('Tu próximo objetivo');
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    expect(await page.locator('body').innerText()).not.toMatch(/familia|JWT|Spring Boot|endpoint|Scrum|proyecto integrador/i);
    if(width<600){await page.getByRole('button',{name:'Menú',exact:true}).click();await expect(page.locator('#public-nav')).toHaveClass(/open/);}
    await page.getByRole('navigation').getByRole('link',{name:'Iniciar sesión'}).click();
    await expect(page.getByRole('heading',{name:'Qué bueno verte.'})).toBeVisible();
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  }
});
test('login incorrecto no abre panel y permite mostrar u ocultar contrasena',async({page})=>{
  await page.goto('/#acceso');
  await page.locator('[name=usuario]').fill('alumno.demo');
  await page.locator('[name=password]').fill('incorrecta');
  await page.getByRole('button',{name:'Mostrar contraseña'}).click();
  await expect(page.locator('[name=password]')).toHaveAttribute('type','text');
  await page.getByRole('button',{name:'Ocultar contraseña'}).click();
  await expect(page.locator('[name=password]')).toHaveAttribute('type','password');
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
  await expect(page.getByRole('alert')).toContainText('Usuario o contraseña incorrectos');
  expect(await page.evaluate(()=>sessionStorage.getItem('umbral.access'))).toBeNull();
  await page.locator('[name=password]').fill(process.env.UMBRAL_WEB_TEST_PASSWORD);
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Bienvenido a UMBRAL.'})).toBeVisible();
});
test('login redirige a cada perfil y conserva sesion al recargar',async({page})=>{
  for(const [user,rol,link] of [['docente.demo','Docente','Banco de preguntas'],['alumno.demo','Alumno','Mis consultas'],['apoderado.demo','Apoderado','Notificaciones']]){
    await page.goto('/#acceso');
    await page.locator('[name=usuario]').fill(user);
    await page.locator('[name=password]').fill(process.env.UMBRAL_WEB_TEST_PASSWORD);
    await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
    await expect(page.locator('.topbar')).toContainText(rol);
    await expect(page.getByRole('navigation').getByRole('link',{name:link,exact:true})).toBeVisible();
    await page.reload();
    await expect(page.locator('.topbar')).toContainText(rol);
    await page.getByRole('button',{name:'Cerrar sesión'}).click();
    await expect(page.locator('#academic-carousel')).toBeVisible();
  }
});
test('carrusel rota automaticamente al quedar fuera del foco',async({page})=>{
  await page.goto('/');
  await page.mouse.move(0,0);
  await expect(page.locator('.carousel-item.active')).not.toContainText('Tu próximo objetivo',{timeout:10000});
});
test('botones tienen elevacion moderada con puntero y foco visible',async({page})=>{
  await page.goto('/');
  const link=page.getByRole('navigation').getByRole('link',{name:'Iniciar sesión'});
  await link.hover();
  await expect.poll(()=>link.evaluate(e=>getComputedStyle(e).transform)).not.toBe('none');
  await link.focus();
  expect(await link.evaluate(e=>e===document.activeElement)).toBe(true);
});
