import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';

/** HU41: el acceso y el reporte principal se consultan al backend demo real, sin tokens manuales. */
async function entrar(page,user='docente.demo'){
  await page.goto('/#acceso');
  await page.locator('[name=usuario]').fill(user);
  await page.locator('[name=password]').fill(process.env.UMBRAL_WEB_TEST_PASSWORD);
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Bienvenido a UMBRAL.'})).toBeVisible();
}
async function abrir(page){
  await entrar(page);
  await page.getByRole('navigation').getByRole('link',{name:'Reportes académicos',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Reportes académicos',exact:true})).toBeVisible();
}
test('HU41 consulta real filtro y descarga PDF valida',async({page},info)=>{
  await abrir(page);
  await expect(page.locator('#report-filters [name=seccion] option')).toHaveCount(1);
  await expect(page.locator('#report-preview')).toContainText('Demo A');
  await expect(page.locator('#report-preview')).toContainText('docente@example.invalid');
  await expect(page.getByRole('table').first().locator('tbody tr')).toHaveCount(2);
  const consulta=page.waitForResponse(r=>r.url().includes('/rendimiento?simulacroId=1'));
  await page.locator('#report-filters [name=simulacro]').selectOption('1');
  const data=await(await consulta).json();
  expect(data.resumen.resultados).toBe(2);
  await expect(page.locator('#report-status')).toHaveText('Resultados actualizados.');
  const descarga=page.waitForEvent('download');
  const peticion=page.waitForResponse(r=>r.url().includes('/rendimiento.pdf?simulacroId=1'));
  await page.getByRole('button',{name:'Exportar PDF',exact:true}).click();
  const response=await peticion;
  expect(response.status()).toBe(200);
  expect(response.headers()['content-type']).toContain('application/pdf');
  const file=await descarga;
  expect(file.suggestedFilename()).toBe('rendimiento-seccion-1.pdf');
  await file.saveAs(info.outputPath('hu41-reporte.pdf'));
  expect((await readFile(info.outputPath('hu41-reporte.pdf'))).subarray(0,5).toString()).toBe('%PDF-');
  expect(await file.failure()).toBeNull();
  await expect(page.getByRole('button',{name:'Exportar PDF',exact:true})).toBeEnabled();
});
test('HU41 sin resultados muestra ausencia de datos y no un promedio cero',async({page})=>{
  // Solo esta prueba fuerza el estado vacío de interfaz; el HTTP del backend lo verifica con base real aparte.
  await page.route('**/api/docente/secciones/1/rendimiento',async route=>{
    const response=await route.fetch();const data=await response.json();
    data.resultados=[];data.temas=[];data.resumen={resultados:0,alumnos:0,promedio:null,minimo:null,maximo:null};
    await route.fulfill({response,json:data});
  });
  await abrir(page);
  await expect(page.getByText('No hay resultados registrados para esta selección. Puedes exportar el reporte con este estado.')).toBeVisible();
  await expect(page.locator('.report-metrics')).toContainText('Sin datos');
});
test('HU41 fallo de exportacion informa error y recupera el boton sin descargar',async({page})=>{
  await abrir(page);
  let downloads=0;page.on('download',()=>downloads++);
  await page.route('**/api/docente/secciones/1/rendimiento.pdf?**',route=>route.fulfill({status:503,contentType:'application/problem+json',body:JSON.stringify({detail:'No se pudo generar el reporte'})}));
  await page.getByRole('button',{name:'Exportar PDF',exact:true}).click();
  await expect(page.getByRole('alert')).toContainText('servicio académico no está disponible');
  await expect(page.getByRole('button',{name:'Exportar PDF',exact:true})).toBeEnabled();
  expect(downloads).toBe(0);
});
test('HU41 consulta fallida bloquea exportar datos anteriores y permite reintentar',async({page})=>{
  await abrir(page);
  await page.route('**/api/docente/secciones/1/rendimiento?**',route=>route.fulfill({status:503,body:'{}'}));
  await page.getByRole('button',{name:'Consultar resultados'}).click();
  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page.getByRole('button',{name:'Exportar PDF',exact:true})).toBeDisabled();
  await page.unroute('**/api/docente/secciones/1/rendimiento?**');
  await page.getByRole('button',{name:'Consultar resultados'}).click();
  await expect(page.locator('#report-status')).toHaveText('Resultados actualizados.');
  await expect(page.getByRole('button',{name:'Exportar PDF',exact:true})).toBeEnabled();
});
test('HU41 alumno y apoderado no tienen acceso al panel docente',async({page})=>{
  for(const user of ['alumno.demo','apoderado.demo']){
    await entrar(page,user);
    await expect(page.getByRole('navigation').getByRole('link',{name:'Reportes académicos'})).toHaveCount(0);
    await page.goto('/#reportes');
    await expect(page.getByText('Esta sección está disponible para docentes.')).toBeVisible();
    await expect(page.locator('#export-report')).toHaveCount(0);
    await page.getByRole('button',{name:'Cerrar sesión'}).click();
  }
});
test('HU41 tablas y filtros se adaptan al movil sin desbordar la pagina',async({page})=>{
  await abrir(page);
  for(const width of [375,768,1440]){
    await page.setViewportSize({width,height:900});
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    await expect(page.getByRole('button',{name:'Exportar PDF',exact:true})).toBeVisible();
  }
});
