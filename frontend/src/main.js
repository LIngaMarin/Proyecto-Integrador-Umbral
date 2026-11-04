import 'bootstrap/dist/css/bootstrap.min.css';
import './style.css';
import { api, session, esc, query } from './api.js';
import { validar } from './componentes.js';
import { resultados as intentos, limpiarResultados } from './practicas.js';
import { landing } from './landing.js';
import { icon } from './icons.js';

// La cuenta confirmada por el backend decide las vistas; ocultar enlaces no sustituye sus permisos.
const app = document.querySelector('#app');
let account;
let generation = 0;
let academicResults=[];
let disposeLanding=()=>{};
// La limpieza es idempotente: Bootstrap no debe disponer dos veces el mismo carrusel.
function clearLanding(){const dispose=disposeLanding;disposeLanding=()=>{};dispose();}
const roles = { DOCENTE: 'Docente', ALUMNO: 'Alumno', APODERADO: 'Apoderado', VISITANTE: 'Visitante' };
const empty = message => `<div class="empty">${esc(message)}</div>`;
const button = (label, attrs = '') => `<button class="btn btn-primary" ${attrs}>${label}</button>`;
const field = (label, name, type = 'text', attrs = '') => `<label>${label}<input class="form-control" name="${name}" type="${type}" ${attrs}></label>`;
const states = '<option value="">Todos los estados</option><option>PENDIENTE</option><option>RESUELTA</option>';
const levels = '<option>FACIL</option><option>INTERMEDIO</option><option>DIFICIL</option>';
const badge = state => `<span class="status ${state === 'PENDIENTE' ? 'pending' : ''}">${esc(state)}</span>`;
// Las fechas sin desplazamiento proceden del calendario académico de Lima, no de la zona del navegador.
const date = value => value ? new Date(/[zZ]$|[+-]\d{2}:\d{2}$/.test(String(value)) ? value : `${value}-05:00`).toLocaleString('es-PE', { timeZone: 'America/Lima' }) : '';
const sectionOptions = () => account.secciones.map((s, i) => `<option value="${i}">${esc(s.nombre)}${s.docente_id ? ` · Docente ${esc(s.docente_id)}` : ''}</option>`).join('');

/** Presenta errores en una región anunciada por lectores de pantalla. */
function notice(message, success = false) {
  const target = document.querySelector('#notice');
  if (target) target.innerHTML = `<div class="alert alert-${success ? 'success' : 'danger'}" role="${success ? 'status' : 'alert'}">${esc(message)}</div>`;
}

/** Bloquea reenvíos durante una operación y restaura controles también si falla la red. */
async function perform(element, operation) {
  const original = element.innerHTML;
  const isButton = element.tagName === 'BUTTON';
  element.disabled = true;
  element.setAttribute('aria-busy', 'true');
  if (isButton) element.textContent = 'Procesando…';
  try { await operation(); } catch (error) { notice(error.message); }
  finally {
    if (element.isConnected) {
      element.disabled = false;
      element.removeAttribute('aria-busy');
      if (isButton) element.innerHTML = original;
    }
  }
}

/** Acceso convencional: únicamente el backend valida la contraseña y devuelve la identidad autorizada. */
function login() {
  clearLanding();
  generation++;
  account = null;
  session.clear();
  limpiarResultados();
  document.title='UMBRAL · Iniciar sesión';
  app.innerHTML = `<main class="access-page"><section class="access-visual"><a href="#portada" class="wordmark">${icon('cap')} UMBRAL</a><img src="/images/estudio.webp" alt="Apuntes de estudio para la preparación universitaria" width="1280" height="740"><div><p class="eyebrow">Tu camino a la universidad</p><h2>Tu esfuerzo de hoy.<br>Tu siguiente oportunidad.</h2><p>Retoma tus prácticas y sigue avanzando hacia tu meta.</p></div></section><section class="access-form"><a class="back-link" href="#portada">← Volver al inicio</a><p class="eyebrow">Campus académico</p><h1>Qué bueno verte.</h1><p class="muted">Ingresa para continuar tu preparación.</p><div id="notice" aria-live="polite"></div><form id="login"><label for="usuario">Correo electrónico o usuario</label><input id="usuario" class="form-control" name="usuario" required maxlength="254" autocomplete="username" autocapitalize="none" spellcheck="false"><label for="password" class="mt-4">Contraseña</label><div class="password-field"><input id="password" class="form-control" name="password" type="password" required maxlength="128" autocomplete="current-password"><button id="toggle-password" type="button" aria-label="Mostrar contraseña" aria-pressed="false">Mostrar</button></div>${button('Iniciar sesión', 'id="login-submit" type="submit"')}</form><p class="muted small mt-4">Si necesitas recuperar tu acceso, comunícate con la administración de tu academia.</p><a href="#practicas">Explorar prácticas públicas ${icon('arrow')}</a></section></main>`;
  document.querySelector('#toggle-password').onclick=event=>{
    const input=document.querySelector('#password');
    const visible=input.type==='password';
    input.type=visible?'text':'password';
    event.currentTarget.textContent=visible?'Ocultar':'Mostrar';
    event.currentTarget.setAttribute('aria-label',visible?'Ocultar contraseña':'Mostrar contraseña');
    event.currentTarget.setAttribute('aria-pressed',String(visible));
  };
  document.querySelector('#login').onsubmit = event => {
    event.preventDefault();
    if(!validar(event.target))return;
    perform(event.submitter, async () => {
      const values=Object.fromEntries(new FormData(event.target));
      const access=await api('/auth/login',{method:'POST',body:values});
      session.set(access.accessToken);
      // El rol se vuelve a obtener con /cuenta; no se confía en campos manipulados del navegador.
      try { account=await api('/cuenta'); } catch(error){session.clear();throw error;}
      event.target.reset();
      history.replaceState(null,'','#inicio');
      await render();
    });
  };
}

/** Portada pública independiente de los paneles: no inventa actividad ni datos académicos. */
function publicHome(){
  generation++;
  clearLanding();
  account=null;
  disposeLanding=landing(app);
}

function shell(page) {
  clearLanding();
  const links = [['inicio', 'Inicio'], ...(account.rol === 'ALUMNO' ? [['practicas', 'Prácticas'], ['consultas', 'Mis consultas'], ['resultados', 'Resultados'], ['calendario', 'Simulacros']] : account.rol === 'DOCENTE' ? [['consultas', 'Consultas académicas'], ['clases', 'Clases y asistencia'], ['banco', 'Banco de preguntas'], ['resultados', 'Resultados']] : account.rol === 'VISITANTE' ? [['practicas', 'Prácticas'], ['acceso', 'Ingresar']] : [['calendario', 'Calendario'], ['notificaciones', 'Notificaciones']]), ...(account.rol === 'VISITANTE' ? [] : [['cuenta', 'Mi cuenta']])];
  app.innerHTML = `<div class="shell"><aside class="sidebar"><div><div class="brand">UMBRAL</div><div class="eyebrow mt-2">Campus académico</div></div><nav aria-label="Navegación principal">${links.map(([id, label]) => `<a href="#${id}" class="${page === id ? 'active' : ''}" ${page === id ? 'aria-current="page"' : ''}>${label}</a>`).join('')}</nav><footer>Tu siguiente paso,<br>cada día más cerca.</footer></aside><div class="workspace"><header class="topbar"><div>${esc(roles[account.rol])}<br><small>${esc(account.email)}</small></div><button id="logout" class="btn btn-outline-secondary">Cerrar sesión</button></header><main><div id="notice" aria-live="polite"></div><section id="content" aria-live="polite"></section></main></div></div>`;
  document.querySelector('#logout').textContent=account.rol==='VISITANTE'?'Iniciar sesión':'Cerrar sesión';
  document.querySelector('#logout').onclick = () => { session.clear(); limpiarResultados(); history.replaceState(null,'',account.rol==='VISITANTE'?'#acceso':'#portada'); if(account.rol==='VISITANTE')login();else publicHome(); };
}

/** Descarta respuestas de una navegación anterior para que no sobrescriban otra pantalla. */
async function render() {
  if (!account) return;
  const current = ++generation;
  const route = location.hash.slice(1) || 'inicio';
  const page = ({ dudas: 'consultas', sesion: 'cuenta' })[route] || route;
  if(page==='portada' || (page==='inicio' && account.rol==='VISITANTE')){publicHome();return;}
  if (page === 'acceso') { login(); return; }
  shell(page);
  const content = document.querySelector('#content');
  content.innerHTML = '<p role="status">Cargando información académica…</p>';
  try {
    const html = await ({ inicio: home, practicas, consultas, banco, resultados, calendario, cuenta, clases, notificaciones }[page] || home)();
    if (current !== generation) return;
    content.innerHTML = html;
    // Las respuestas se consultan de nuevo al navegar para reflejar su persistencia real.
    bind(page);
    content.tabIndex = -1;
    content.focus({ preventScroll: true });
    window.scrollTo({ top: 0, behavior: 'instant' });
    document.title = `UMBRAL · ${content.querySelector('h1')?.textContent || 'Campus académico'}`;
  } catch (error) { if (current === generation) { content.innerHTML = empty('No se pudo cargar esta sección.'); notice(error.message); } }
}

async function home() {
  let metrics = '';
  if (account.rol === 'ALUMNO') {
    const doubts = await api('/alumno/dudas');
    metrics = [['Consultas registradas', doubts.length], ['Pendientes', doubts.filter(d => d.estado === 'PENDIENTE').length], ['Resueltas', doubts.filter(d => d.estado === 'RESUELTA').length]].map(([label, value]) => `<article class="panel"><p class="eyebrow">${label}</p><div class="metric">${value}</div></article>`).join('');
  } else if (account.rol === 'DOCENTE') {
    const groups = await api('/docente/dudas/grupos');
    metrics = `<article class="panel"><p class="eyebrow">Temas de consulta</p><div class="metric">${groups.length}</div></article><article class="panel"><p class="eyebrow">Secciones asignadas</p><div class="metric">${account.secciones.length}</div></article>`;
  } else metrics = `<article class="panel"><p class="eyebrow">Alumnos vinculados</p><div class="metric">${account.hijos.length}</div></article>`;
  const tools=account.rol==='ALUMNO'?[['book','Prácticas','Refuerza un tema y resuelve tu siguiente pregunta.','practicas'],['chat','Mis consultas','Revisa las respuestas y continúa aprendiendo.','consultas'],['chart','Mis resultados','Revisa tus respuestas y el historial de tus prácticas.','resultados'],['calendar','Simulacros','Consulta las fechas de tu preparación académica.','calendario']]:account.rol==='DOCENTE'?[['chat','Consultas académicas','Atiende los temas que necesitan tus alumnos.','consultas'],['book','Banco de preguntas','Revisa y publica material académico.','banco'],['calendar','Clases y asistencia','Organiza tus clases y registra la presencia.','clases'],['chart','Resultados y reportes','Consulta estadísticas y descarga informes.','resultados']]:[['calendar','Calendario','Consulta los simulacros de alumnos vinculados.','calendario'],['bell','Notificaciones','Revisa avisos académicos de asistencia.','notificaciones']];
  return `<p class="eyebrow">Tu espacio académico · ${esc(roles[account.rol])}</p><h1>Bienvenido a UMBRAL.</h1><p class="muted">${esc(account.email)}</p><div class="hero"><div><p class="eyebrow">${account.rol==='ALUMNO'?'Tu próximo objetivo está más cerca':'Acompañamiento académico'}</p><h2>${account.rol==='ALUMNO'?'Sigue construyendo tu camino.':account.rol==='DOCENTE'?'Enseñar también es abrir caminos.':'Sigue su preparación.'}</h2><p>${account.rol==='ALUMNO'?'Retoma tus prácticas, comparte tus dudas y fortalece lo que aprendes.':account.rol==='DOCENTE'?'Revisa las consultas de tus secciones y orienta el siguiente paso de tus alumnos.':'Consulta las actividades y avisos de los alumnos vinculados a tu cuenta.'}</p><a class="btn btn-primary" href="#${tools[0][3]}">${account.rol==='ALUMNO'?'Continuar practicando':account.rol==='DOCENTE'?'Atender consultas':'Ver calendario'} ${icon('arrow')}</a></div><span class="hero-symbol">${icon('cap')}</span></div><div class="cards">${metrics}</div><div class="section-intro mt-5"><div><p class="eyebrow">A un paso</p><h2>Tus herramientas académicas</h2></div></div><div class="tool-grid">${tools.map(([symbol,title,text,href])=>`<article class="tool-card"><span class="tool-icon">${icon(symbol)}</span><h3>${title}</h3><p>${text}</p><a href="#${href}">Abrir ${icon('arrow')}</a></article>`).join('')}</div>`;
}

async function cuenta() {
  return `<p class="eyebrow">Perfil académico</p><h1>Mi cuenta</h1><article class="panel"><h2>${esc(account.email)}</h2><p>${esc(roles[account.rol])}</p><h3>Mis ${account.rol === 'APODERADO' ? 'alumnos vinculados' : 'secciones'}</h3>${account.rol === 'APODERADO' ? account.hijos.map(h => `<p>${esc(h.email)}</p>`).join('') || empty('No tienes alumnos vinculados.') : account.secciones.map(s => `<p>${esc(s.nombre)}</p>`).join('') || empty('No tienes secciones asignadas.')}</article>`;
}

async function practicas() {
  if (!['ALUMNO', 'VISITANTE'].includes(account.rol)) return empty('Esta sección está disponible para alumnos.');
  return `<p class="eyebrow">Aprende a tu ritmo</p><h1>Prácticas</h1><p class="muted">Elige un tema y pon a prueba lo que aprendiste.</p><form id="practice-search" class="panel filters">${field('Tema', 'tema', 'text', 'required maxlength="255" placeholder="Nombre del tema"')}<label>Dificultad<select class="form-select" name="dificultad">${levels}</select></label>${button('Buscar preguntas')}</form><div id="questions">${empty('Selecciona un tema para comenzar.')}</div>`;
}

function doubtRow(d, teacher = false) {
  return `<article class="question"><div class="d-flex justify-content-between gap-3">${badge(d.estado)}<small class="muted">${esc(date(d.fecha))}</small></div><p class="preserve mt-3">${esc(d.texto)}</p><div class="actions">${d.respuestaId && !teacher ? button('Ver respuesta', `data-answer="${d.id}"`) : ''}${d.respuestaId && d.estado !== 'RESUELTA' ? button('Marcar como resuelta', `data-resolve="${d.id}"`) : ''}<button class="btn btn-outline-secondary" data-history="${d.id}">Ver historial</button></div><div id="detail-${d.id}" class="mt-3 preserve"></div></article>`;
}

async function consultas() {
  if (account.rol === 'ALUMNO') {
    const doubts = await api('/alumno/dudas');
    return `<p class="eyebrow">Aprende con orientación</p><h1>Mis consultas académicas</h1><article class="panel"><h2>Nueva consulta</h2>${account.secciones.length ? `<form id="new-doubt"><div class="filters"><label>Sección y docente<select class="form-select" name="seccion">${sectionOptions()}</select></label>${field('Tema', 'tema', 'text', 'required maxlength="255"')}</div><label class="w-100 mt-3">Tu consulta<textarea class="form-control" name="texto" required maxlength="4000" rows="3"></textarea></label>${button('Enviar consulta', 'type="submit"')}</form>` : empty('Necesitas una matrícula activa para enviar consultas.')}</article><article class="panel"><div class="filters"><h2>Mis consultas</h2><label>Estado<select id="doubt-state" class="form-select">${states}</select></label></div><div id="doubts">${doubts.map(d => doubtRow(d)).join('') || empty('Todavía no has registrado consultas.')}</div></article>`;
  }
  if (account.rol !== 'DOCENTE') return empty('Esta sección está disponible para alumnos y docentes.');
  const groups = await api('/docente/dudas/grupos');
  // Los recuentos se calculan con las dudas reales de cada grupo autorizado.
  const details = await Promise.all(groups.map(async g => ({ ...g, dudas: await api(`/docente/dudas/grupos/${g.id}`) })));
  window.umbralGroups = details;
  return `<p class="eyebrow">Acompañamiento académico</p><h1>Centro de consultas académicas</h1><p class="muted">Atiende las preguntas relacionadas de tus alumnos desde un mismo lugar.</p><div class="panel filters">${field('Buscar tema', 'tema', 'search', 'id="group-search"')}<label>Estado<select id="group-state" class="form-select">${states}</select></label></div><div id="groups">${groupsHtml(details)}</div>`;
}

function groupsHtml(groups, term = '', state = '') {
  const visible = groups.filter(g => g.tema.toLocaleLowerCase().includes(term.toLocaleLowerCase()) && (!state || g.dudas.some(d => d.estado === state)));
  return visible.map(g => `<article class="panel"><div class="d-flex justify-content-between"><h2>${esc(g.tema)}</h2><span class="status">${g.dudas.length} consultas</span></div><p class="muted">${esc(account.secciones.find(s => s.id === g.seccionId)?.nombre || 'Sección asignada')} · ${g.dudas.filter(d => d.estado === 'PENDIENTE').length} pendientes</p><details><summary>Ver preguntas relacionadas</summary>${g.dudas.filter(d => !state || d.estado === state).map(d => doubtRow(d, true)).join('')}<button class="btn btn-outline-secondary mt-2" data-group-history="${g.id}">Ver respuestas del grupo</button><div id="group-history-${g.id}" class="preserve mt-3"></div></details>${g.dudas.some(d => d.estado === 'PENDIENTE' && !d.respuestaId) ? `<form data-group-response="${g.id}" class="mt-4"><label class="w-100">Respuesta para las consultas sin atender<textarea class="form-control" name="texto" required maxlength="10000" rows="3"></textarea></label>${button('Enviar respuesta al grupo')}</form>` : '<p class="muted mt-3">Las consultas de este grupo ya fueron atendidas.</p>'}</article>`).join('') || empty('No hay consultas para estos filtros.');
}

async function banco() {
  if (account.rol !== 'DOCENTE') return empty('Esta sección está disponible para docentes.');
  const questions = await api(`/docente/preguntas?${query({ docenteId: account.id })}`);
  return `<p class="eyebrow">Material académico</p><h1>Banco de preguntas</h1><p class="muted">Revisa las preguntas y publica el material aprobado para tus alumnos.</p><article class="panel"><h2>Importar boletín</h2><p class="muted">Selecciona un PDF con preguntas, opciones y respuestas para revisarlas antes de publicarlas.</p><form id="upload-bulletin" class="filters"><label>Boletín PDF (hasta 1 MB)<input class="form-control" type="file" name="file" accept="application/pdf,.pdf" required></label>${button('Importar preguntas')}</form><details class="mt-3"><summary>Formato del boletín</summary><p>Usa un bloque por pregunta, con texto seleccionable y estos campos:</p><pre>Tema: Algebra
Dificultad: FACIL
Pregunta: Resuelve 2x + 6 = 14
A) 2
B) 4
C) 6
D) 8
Correcta: B</pre><p>Los niveles admitidos son FACIL, INTERMEDIO y DIFICIL. Las imágenes escaneadas necesitan convertirse en texto antes de importar.</p></details></article><form id="bank-search" class="filters panel">${field('Tema', 'tema')}<label>Dificultad<select class="form-select" name="dificultad"><option value="">Todas</option>${levels}</select></label>${button('Filtrar')}</form><div id="bank-list">${bankHtml(questions)}</div>`;
}

function bankHtml(questions) {
  return questions.map(p => `<article class="panel"><div class="d-flex justify-content-between"><h2>${esc(p.tema)}</h2>${badge(p.estado)}</div><form data-question="${p.id}"><label class="w-100">Enunciado<textarea class="form-control" name="enunciado" required>${esc(p.enunciado)}</textarea></label><div class="filters mt-2">${field('Tema', 'tema', 'text', `required value="${esc(p.tema)}"`)}<label>Dificultad<select class="form-select" name="dificultad">${['FACIL', 'INTERMEDIO', 'DIFICIL'].map(l => `<option ${l === p.dificultad ? 'selected' : ''}>${l}</option>`).join('')}</select></label></div><label class="w-100 mt-3">Opciones (una por línea)<textarea class="form-control" name="opciones" required>${esc(p.opciones.join('\n'))}</textarea></label>${field('Respuesta correcta', 'respuestaCorrecta', 'text', `required value="${esc(p.respuestaCorrecta)}"`)}<div class="actions">${button('Guardar y aprobar')}<button type="button" class="btn btn-outline-secondary" data-solution="${p.id}">Generar solucionario</button><button type="button" class="btn btn-outline-secondary" data-review-solution="${p.id}">Revisar solucionario</button><button type="button" class="btn btn-outline-secondary" data-variant="${p.id}">Crear variante de ecuación</button></div></form><form data-reject="${p.id}" class="filters mt-3">${field('Motivo de rechazo', 'motivoRechazo', 'text', 'required maxlength="255"')}<button class="btn btn-outline-danger">Rechazar</button></form><div id="solution-${p.id}" class="preserve mt-3"></div></article>`).join('') || empty('No se encontraron preguntas.');
}

async function resultados() {
  if (account.rol === 'ALUMNO') {
    const attempts=await api('/practicas/respuestas');
    academicResults=attempts;
    return `<p class="eyebrow">Aprende de cada intento</p><h1>Mis resultados</h1><p class="muted">Tus respuestas y avances registrados en las prácticas.</p><div class="cards"><article class="panel"><p class="eyebrow">Intentos registrados</p><div class="metric">${attempts.length}</div></article><article class="panel"><p class="eyebrow">Respuestas correctas</p><div class="metric">${attempts.filter(r=>r.correcta).length}</div></article></div><article class="panel"><h2>Historial de prácticas</h2><label>Resultado<select id="result-state" class="form-select"><option value="">Todos</option><option value="correcta">Correctas</option><option value="incorrecta">Por reforzar</option></select></label><div id="result-list">${resultsHtml(attempts)}</div></article>`;
  }
  if (account.rol !== 'DOCENTE') return empty('Esta sección está disponible para docentes.');
  const summary = await api('/docente/resumen-semanal');
  return `<p class="eyebrow">Seguimiento del aprendizaje</p><h1>Resultados y reportes</h1><article class="panel"><h2>Resumen semanal</h2><form id="weekly" class="filters">${field('Semana (lunes)', 'semana', 'date', `required value="${summary.inicio}" step="7"`)}${button('Consultar')}</form><div id="weekly-result" class="mt-3">${summaryHtml(summary)}</div></article><article class="panel"><h2>Rendimiento por sección</h2>${account.secciones.length ? `<form id="section-results" class="filters"><label>Sección<select class="form-select" name="seccion">${sectionOptions()}</select></label>${button('Consultar temas por reforzar')}<button type="button" class="btn btn-outline-secondary" id="download-pdf">Descargar informe PDF</button></form><div id="weak-topics" class="mt-3"></div>` : empty('No tienes secciones asignadas.')}</article>`;
}

function summaryHtml(s) {
  const labels = { preguntasCreadas: 'Preguntas creadas', preguntasCreadasActualmenteAprobadas: 'Preguntas aprobadas', intentosPractica: 'Intentos de práctica', aciertosPractica: 'Aciertos', alumnosPractica: 'Alumnos que practicaron', dudasRegistradas: 'Consultas registradas', respuestasAgrupadas: 'Respuestas grupales', dudasResueltas: 'Consultas resueltas' };
  return `<p class="muted">Desde ${esc(s.inicio)} hasta ${esc(s.finExclusivo)} (fin no incluido)</p><div class="table-wrap"><table class="table"><tbody>${Object.entries(labels).map(([key, label]) => `<tr><th scope="row">${label}</th><td>${esc(s[key])}</td></tr>`).join('')}</tbody></table></div>`;
}

/** El historial muestra la evaluación guardada por el servidor, nunca calcula una nota local. */
function resultsHtml(rows){
  return rows.map(r=>`<article class="question"><div class="d-flex justify-content-between gap-3"><span class="status">${r.correcta?'Correcta':'Por reforzar'}</span><small class="muted">${esc(date(r.fecha))}</small></div><h3 class="mt-3 preserve">${esc(r.enunciado)}</h3><p class="muted">${esc(r.tema)}</p><p class="preserve">Tu respuesta: ${esc(r.respuesta)}</p></article>`).join('')||empty('Todavía no tienes intentos registrados para este filtro.');
}

async function calendario() {
  if (account.rol === 'ALUMNO') return `<p class="eyebrow">Organiza tu preparación</p><h1>Simulacros y calendario</h1><p class="muted">Consulta las fechas de los simulacros de tus cursos.</p><form id="calendar-search" class="panel filters">${field('Desde','desde','date','required')}${field('Hasta','hasta','date','required')}${button('Consultar calendario')}</form><div id="calendar-list">${empty('Selecciona un período para consultar tus simulacros.')}</div>`;
  if (account.rol !== 'APODERADO') return empty('Esta sección está disponible para apoderados.');
  return `<p class="eyebrow">Seguimiento académico</p><h1>Calendario académico</h1><p class="muted">Consulta las próximas fechas de simulacros de tus alumnos vinculados.</p>${account.hijos.length ? `<form id="calendar-search" class="panel filters"><label>Alumno<select class="form-select" name="alumno">${account.hijos.map(h => `<option value="${h.id}">${esc(h.email)}</option>`).join('')}</select></label>${field('Desde', 'desde', 'date', 'required')}${field('Hasta', 'hasta', 'date', 'required')}${button('Consultar calendario')}</form><div id="calendar-list">${empty('Selecciona un rango de fechas para consultar las actividades.')}</div>` : empty('No tienes alumnos vinculados.')}`;
}

/** La lista de asistencia sale de las matrículas reales; no se escriben alumnos manualmente. */
async function clases() {
  if (account.rol !== 'DOCENTE') return empty('Esta sección está disponible para docentes.');
  const list = await api('/docente/clases');
  return `<p class="eyebrow">Acompañamiento diario</p><h1>Clases y asistencia</h1><p class="muted">Registra la presencia antes del cierre de la clase.</p><article class="panel"><h2>Programar clase</h2>${account.secciones.length ? `<form id="new-class"><div class="filters"><label>Sección<select class="form-select" name="seccion">${sectionOptions()}</select></label>${field('Nombre de la clase', 'titulo', 'text', 'required maxlength="150"')}${field('Finalización (hora de Lima)', 'fin', 'datetime-local', 'required')}</div>${button('Programar clase', 'type="submit"')}</form>` : empty('No tienes secciones asignadas.')}</article>${list.map(c => `<article class="panel"><div class="d-flex justify-content-between"><h2>${esc(c.titulo)}</h2>${badge(c.cerrada ? 'CERRADA' : 'ABIERTA')}</div><p class="muted">Finalización: ${esc(date(c.fin))} · ${esc(account.secciones.find(s => s.id === c.seccion_id)?.nombre)}</p><div class="actions"><button class="btn btn-outline-secondary" data-class-students="${c.id}" data-class-closed="${c.cerrada}">Ver asistencia</button>${!c.cerrada ? button('Cerrar asistencia', `data-close-class="${c.id}"`) : ''}</div><div id="class-${c.id}" class="mt-3"></div></article>`).join('') || empty('Todavía no tienes clases programadas.')}`;
}

/** Solo ofrece hijos vinculados; el servidor vuelve a comprobar el vínculo en cada consulta. */
async function notificaciones() {
  if (account.rol !== 'APODERADO') return empty('Esta sección está disponible para apoderados.');
  const lists = await Promise.all(account.hijos.map(async h => ({ alumno: h, items: await api(`/apoderado/alumnos/${h.id}/notificaciones`) })));
  return `<p class="eyebrow">Seguimiento académico</p><h1>Notificaciones académicas</h1><p class="muted">Avisos sobre la asistencia de tus alumnos vinculados.</p>${lists.map(({ alumno, items }) => `<article class="panel"><h2>${esc(alumno.email)}</h2>${items.map(n => `<div class="question"><small class="muted">${esc(date(n.fecha))}</small><p class="preserve">${esc(n.descripcion)}</p></div>`).join('') || empty('No tienes notificaciones de este alumno.')}</article>`).join('') || empty('No tienes alumnos vinculados.')}`;
}

/** Conecta únicamente controles de la vista actual con sus operaciones reales. */
function bind(page) {
  const form = (selector, callback) => document.querySelectorAll(selector).forEach(f => { f.onsubmit = event => { event.preventDefault(); if (!validar(f)) return; const values = Object.fromEntries(new FormData(f)); perform(event.submitter, () => callback(values, f)); }; });
  const click = (selector, callback) => document.querySelectorAll(selector).forEach(b => { b.onclick = () => perform(b, () => callback(b)); });
  const resultFilter=document.querySelector('#result-state');
  if(resultFilter)resultFilter.onchange=()=>{document.querySelector('#result-list').innerHTML=resultsHtml(academicResults.filter(r=>!resultFilter.value||r.correcta===(resultFilter.value==='correcta')));};
  // La importación usa multipart real; solo después de la confirmación se vuelve a consultar el banco.
  form('#upload-bulletin', async (_values,f)=>{
    const body=new FormData(f);
    const file=body.get('file');
    if(!file || file.size===0 || file.size>1048576)throw new Error('Selecciona un PDF de hasta 1 MB.');
    body.append('docenteId',String(account.id));
    const imported=await api('/docente/boletin/extraer',{method:'POST',body});
    await render();notice(`${imported.length} preguntas importadas para revisión.`,true);
  });
  click('[data-variant]',async b=>{
    await api(`/docente/preguntas/${b.dataset.variant}/variantes?${query({docenteId:account.id})}`,{method:'POST'});
    await render();notice('La variante se guardó para revisión.',true);
  });
  form('#new-class', async values => { const s = account.secciones[Number(values.seccion)]; await api('/docente/clases', { method: 'POST', body: { seccionId: s.id, titulo: values.titulo, fin: values.fin + ':00' } }); await render(); notice('La clase fue programada.', true); });
  click('[data-close-class]', async b => { await api(`/docente/clases/${b.dataset.closeClass}/cerrar`, { method: 'PUT' }); await render(); notice('Asistencia cerrada. Los avisos están disponibles para los apoderados vinculados.', true); });
  click('[data-class-students]', async b => {
    const alumnos = await api(`/docente/clases/${b.dataset.classStudents}/alumnos`);
    document.querySelector(`#class-${b.dataset.classStudents}`).innerHTML = alumnos.map(a => `<div class="d-flex justify-content-between align-items-center question"><span>${esc(a.email)}</span>${a.presente ? badge('PRESENTE') : b.dataset.classClosed === 'true' ? badge('SIN ASISTENCIA') : `<button class="btn btn-outline-secondary" data-presente="${a.id}" data-class-id="${b.dataset.classStudents}">Registrar presente</button>`}</div>`).join('') || empty('No hay alumnos matriculados.');
    bind('attendance-list');
  });
  click('[data-presente]', async b => { await api(`/docente/clases/${b.dataset.classId}/presentes/${b.dataset.presente}`, { method: 'PUT' }); b.outerHTML = badge('PRESENTE'); });
  form('#practice-search', async values => {
    const questions = await api(`/practicas/preguntas?${query(values)}`);
    document.querySelector('#questions').innerHTML = questions.map(p => `<article class="panel"><span class="status">${esc(p.dificultad)}</span><h2 class="mt-3 preserve">${esc(p.enunciado)}</h2><form data-practice="${p.id}">${p.opciones.map((o, i) => `<label class="d-block p-2"><input type="radio" name="respuesta" value="${esc(o)}" required> ${String.fromCharCode(65 + i)}. ${esc(o)}</label>`).join('')}<div class="actions">${button('Enviar respuesta')}<button type="button" class="btn btn-outline-secondary" data-student-solution="${p.id}">Ver solucionario</button></div></form><div id="attempt-${p.id}" class="preserve mt-3"></div></article>`).join('') || empty('No hay preguntas publicadas para este tema y dificultad.');
    bind('practice-list');
  });
  form('[data-practice]', async (values, f) => {
    if (account.rol === 'VISITANTE') throw new Error('Ingresa con tu acceso de alumno para enviar respuestas.');
    const r = await api('/practicas/respuestas', { method: 'POST', body: { preguntaId: Number(f.dataset.practice), respuesta: values.respuesta } });
    intentos.push({ ...r, enunciado: f.closest('article').querySelector('h2').textContent });
    document.querySelector(`#attempt-${f.dataset.practice}`).textContent = r.correcta ? 'Respuesta correcta. ¡Sigue avanzando!' : 'Respuesta registrada. Revisa el solucionario para reforzar el tema.';
  });
  click('[data-student-solution]', async b => { const s = await api(`/practicas/preguntas/${b.dataset.studentSolution}/solucionario`); document.querySelector(`#attempt-${b.dataset.studentSolution}`).textContent = `${s.pasoAPaso}${s.explicacionErroresComunes ? '\n' + s.explicacionErroresComunes : ''}`; });
  form('#new-doubt', async values => { const s = account.secciones[Number(values.seccion)]; await api('/alumno/dudas', { method: 'POST', body: { docenteId: s.docente_id, seccionId: s.id, tema: values.tema, texto: values.texto } }); await render(); notice('Tu consulta fue registrada.', true); });
  const doubtState = document.querySelector('#doubt-state');
  if (doubtState) doubtState.onchange = () => perform(doubtState, async () => { const ds = await api(`/alumno/dudas?${query({ estado: doubtState.value })}`); document.querySelector('#doubts').innerHTML = ds.map(d => doubtRow(d)).join('') || empty('No hay consultas con este estado.'); bind('doubt-list'); });
  const groupSearch = document.querySelector('#group-search');
  if (groupSearch) {
    const update = () => { document.querySelector('#groups').innerHTML = groupsHtml(window.umbralGroups, groupSearch.value, document.querySelector('#group-state').value); bind('group-list'); };
    groupSearch.oninput = update;
    document.querySelector('#group-state').onchange = update;
  }
  const prefix = account.rol === 'DOCENTE' ? '/docente' : '/alumno';
  click('[data-answer]', async b => { const r = await api(`/alumno/dudas/${b.dataset.answer}/respuesta`); document.querySelector(`#detail-${b.dataset.answer}`).textContent = r.texto; });
  click('[data-history]', async b => { const history = await api(`${prefix}/dudas/${b.dataset.history}/historial`); document.querySelector(`#detail-${b.dataset.history}`).textContent = history.map(h => `${date(h.fecha)} · ${h.evento}`).join('\n'); });
  click('[data-resolve]', async b => { await api(`${prefix}/dudas/${b.dataset.resolve}/resolver`, { method: 'PUT' }); await render(); notice('La consulta quedó resuelta.', true); });
  form('[data-group-response]', async (values, f) => { await api(`/docente/dudas/grupos/${f.dataset.groupResponse}/respuestas`, { method: 'POST', body: values }); await render(); notice('La respuesta se guardó para las consultas del grupo.', true); });
  click('[data-group-history]', async b => { const responses = await api(`/docente/dudas/grupos/${b.dataset.groupHistory}/respuestas`); document.querySelector(`#group-history-${b.dataset.groupHistory}`).textContent = responses.map(r => `${date(r.fecha)}\n${r.texto}`).join('\n\n') || 'Todavía no hay respuestas.'; });
  form('#bank-search', async values => { const qs = await api(`/docente/preguntas?${query({ ...values, docenteId: account.id })}`); document.querySelector('#bank-list').innerHTML = bankHtml(qs); bind('bank-list'); });
  form('[data-question]', async (v, f) => {
    const opciones = v.opciones.split('\n').map(o => o.trim()).filter(Boolean);
    if (opciones.length < 2 || !opciones.includes(v.respuestaCorrecta)) throw new Error('Introduce al menos dos opciones y una respuesta correcta que coincida con una opción.');
    await api(`/docente/preguntas/${f.dataset.question}/aprobar`, { method: 'PUT', body: { ...v, opciones } }); await render(); notice('Pregunta guardada y aprobada.', true);
  });
  form('[data-reject]', async (values, f) => { await api(`/docente/preguntas/${f.dataset.reject}/rechazar`, { method: 'PUT', body: values }); await render(); notice('Pregunta rechazada.', true); });
  click('[data-solution]', async b => { const s = await api(`/docente/preguntas/${b.dataset.solution}/solucionario`, { method: 'POST' }); document.querySelector(`#solution-${b.dataset.solution}`).textContent = `Solucionario ${s.id} · ${s.estado}\n${s.pasoAPaso}`; });
  click('[data-review-solution]', async b => {
    const s = await api(`/docente/preguntas/${b.dataset.reviewSolution}/solucionario`);
    document.querySelector(`#solution-${b.dataset.reviewSolution}`).innerHTML = `<p>${esc(s.estado)}</p><p class="preserve">${esc(s.pasoAPaso)}</p>${s.estado === 'BORRADOR' ? button('Aprobar solucionario revisado', `data-approve-solution="${s.id}"`) : ''}`;
    bind('solution-review');
  });
  click('[data-approve-solution]', async b => {
    await api(`/docente/solucionarios/${b.dataset.approveSolution}/aprobar`, { method: 'PUT' });
    b.parentElement.querySelector('p').textContent = 'APROBADO';
    const message = document.createElement('p');
    message.textContent = 'Solucionario aprobado.';
    b.replaceWith(message);
  });
  form('#approve-solution', async values => { const s = await api(`/docente/solucionarios/${Number(values.id)}/aprobar`, { method: 'PUT' }); document.querySelector('#solution-review').textContent = `Solucionario ${s.id} · ${s.estado}\n${s.pasoAPaso}`; });
  form('#weekly', async values => { const s = await api(`/docente/resumen-semanal?${query(values)}`); document.querySelector('#weekly-result').innerHTML = summaryHtml(s); });
  form('#section-results', async values => { const s = account.secciones[Number(values.seccion)]; const topics = await api(`/docente/secciones/${s.id}/temas-debiles`); document.querySelector('#weak-topics').innerHTML = topics.length ? `<table class="table"><thead><tr><th>Tema</th><th>Errores registrados</th></tr></thead><tbody>${topics.map(t => `<tr><td>${esc(t.tema)}</td><td>${esc(t.cantidadErrores)}</td></tr>`).join('')}</tbody></table>` : empty('Todavía no hay temas por reforzar registrados.'); });
  click('#download-pdf', async () => { const f = document.querySelector('#section-results'); const s = account.secciones[Number(new FormData(f).get('seccion'))]; const blob = await api(`/docente/secciones/${s.id}/rendimiento.pdf`, { blob: true }); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `rendimiento-seccion-${s.id}.pdf`; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); });
  form('#calendar-search', async values => { if (values.desde > values.hasta) throw new Error('La fecha final debe ser igual o posterior a la inicial.'); const base=account.rol==='ALUMNO'?'/alumno/simulacros':`/apoderado/alumnos/${values.alumno}/simulacros`; const events = await api(`${base}?${query({ desde: values.desde, hasta: values.hasta })}`); document.querySelector('#calendar-list').innerHTML = events.map(e => `<article class="panel"><p class="eyebrow">${esc(e.fecha)}</p><h2>${esc(e.nombre)}</h2></article>`).join('') || empty('Todavía no hay actividades programadas en este período.'); });
}

/** Rutas públicas y privadas: los enlaces protegidos piden acceso y conservan la autorización del servidor. */
function navigate(){
  const path=location.hash.slice(1);
  if(account && account.rol!=='VISITANTE'){render();return;}
  if(path==='practicas'){account={rol:'VISITANTE',email:'Catálogo público',secciones:[],hijos:[]};render();}
  else if(['acceso','consultas','resultados','calendario','cuenta','dudas','sesion'].includes(path)){login();}
  else if(path==='herramientas'){if(!document.querySelector('#herramientas'))publicHome(); document.querySelector('#herramientas')?.scrollIntoView({behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'});}
  else publicHome();
}
async function start() { account = await api('/cuenta'); await render(); }
window.addEventListener('hashchange', navigate);
window.addEventListener('session-expired', login);
if (session.get()) start().catch(error => { login(); notice(error.message); }); else navigate();
