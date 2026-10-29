import { api } from './api.js';
import { encabezado, escapar, vacio, tarea, validar } from './componentes.js';

// Solo se almacenan intentos confirmados por el servidor durante esta sesión.
export const resultados = [];
export function limpiarResultados() { resultados.length = 0; }

/** Consulta el catálogo público con los valores de dificultad del enum Kotlin. */
export function practicas(main) {
  main.innerHTML = encabezado('Prácticas académicas', 'Elige un tema y encuentra tu próximo desafío.') + `
    <!-- Filtros obligatorios del catálogo público de Spring Boot. -->
    <section class="card mb-4"><div class="card-body"><form id="filtros" class="row g-3 align-items-end">
      <div class="col-md-6"><label class="form-label" for="tema">Tema</label><input class="form-control" id="tema" name="tema" type="text" maxlength="255" placeholder="Ej. Algebra" required><div class="form-text">Escribe el tema tal como está registrado en el catálogo.</div></div>
      <div class="col-md-3"><label class="form-label" for="dificultad">Dificultad</label><select id="dificultad" name="dificultad" class="form-select"><option value="FACIL">Fácil</option><option value="INTERMEDIO">Intermedio</option><option value="DIFICIL">Difícil</option></select></div>
      <div class="col-md-3"><button class="btn btn-primary w-100">Buscar preguntas</button></div>
    </form></div></section><div id="preguntas" aria-live="polite">${vacio('Selecciona un tema y una dificultad para consultar preguntas reales.')}</div>`;
  const form = main.querySelector('#filtros');
  const lista = main.querySelector('#preguntas');
  form.addEventListener('submit', event => {
    event.preventDefault();
    if (!validar(form)) return;
    tarea(form.querySelector('button'), lista, async () => {
      const preguntas = await api.preguntas(form.elements.tema.value.trim(), form.elements.dificultad.value);
      if (!Array.isArray(preguntas)) throw new Error('El catálogo recibido no tiene el formato esperado.');
      lista.innerHTML = preguntas.length ? `<p class="text-secondary">${preguntas.length} pregunta(s) encontrada(s)</p>` : vacio('No hay preguntas aprobadas para estos filtros. Prueba otro tema o dificultad.');
      preguntas.forEach(pregunta => lista.append(crearPregunta(pregunta)));
    });
  });
}

/** Construye opciones y envía la respuesta elegida; la evaluación procede del backend. */
function crearPregunta(pregunta) {
  const tarjeta = document.createElement('article');
  tarjeta.className = 'card mb-4';
  tarjeta.innerHTML = `<div class="card-body"><div class="d-flex gap-2 mb-3"><span class="badge text-bg-light">${escapar(pregunta.tema)}</span><span class="badge text-bg-light">${escapar(pregunta.dificultad)}</span></div><h2 class="h5 question-text">${escapar(pregunta.enunciado)}</h2><form class="mt-4"><fieldset><legend class="fs-6">Selecciona una respuesta</legend><div class="d-grid gap-2 opciones"></div></fieldset><button class="btn btn-primary mt-3">Enviar respuesta</button></form><button class="btn btn-outline-dark mt-3 solucion" type="button">Consultar solucionario</button><div class="mt-3 feedback" aria-live="polite"></div></div>`;
  const form = tarjeta.querySelector('form');
  const opciones = tarjeta.querySelector('.opciones');
  for (const [indice, opcion] of (pregunta.opciones || []).entries()) {
    const etiqueta = document.createElement('label');
    etiqueta.className = 'option';
    const input = document.createElement('input');
    Object.assign(input, { type: 'radio', name: 'respuesta', value: opcion, required: true });
    etiqueta.append(input, document.createTextNode(`${String.fromCharCode(65 + indice)}. ${opcion}`));
    opciones.append(etiqueta);
  }
  if (!opciones.children.length) {
    form.innerHTML = vacio('Esta pregunta no contiene opciones disponibles.');
  } else form.addEventListener('submit', event => {
    event.preventDefault();
    if (!validar(form)) return;
    tarea(form.querySelector('button'), tarjeta.querySelector('.feedback'), async () => {
      const intento = await api.responder({ preguntaId: pregunta.id, respuesta: new FormData(form).get('respuesta') });
      if (typeof intento.correcta !== 'boolean') throw new Error('El servidor no devolvió una evaluación válida.');
      resultados.push({ ...intento, enunciado: pregunta.enunciado });
      tarjeta.querySelector('.feedback').innerHTML = `<div role="status" class="alert alert-${intento.correcta ? 'success' : 'warning'}">${intento.correcta ? '¡Respuesta correcta!' : 'Respuesta incorrecta. Consulta el solucionario para aprender del intento.'} <a href="#resultados">Ver resultados</a></div>`;
    });
  });
  tarjeta.querySelector('.solucion').addEventListener('click', event => tarea(event.currentTarget, tarjeta.querySelector('.feedback'), async () => {
    const solucion = await api.solucion(pregunta.id);
    tarjeta.querySelector('.feedback').innerHTML = `<section class="border rounded p-3"><h3 class="h6">Solucionario aprobado</h3><p>Respuesta: ${escapar(solucion.respuestaCorrecta)}</p><p class="user-text">${escapar(solucion.pasoAPaso)}</p>${solucion.explicacionErroresComunes ? `<p class="user-text">${escapar(solucion.explicacionErroresComunes)}</p>` : ''}</section>`;
  }));
  return tarjeta;
}

/** Presenta el historial temporal sin simular notas ni persistencia en una API inexistente. */
export function verResultados(main) {
  main.innerHTML = encabezado('Tus resultados', 'Intentos confirmados en esta sesión. Se eliminan al recargar o cambiar el token; el historial global aún no tiene un endpoint disponible.');
  if (!resultados.length) { main.innerHTML += vacio('Todavía no has enviado respuestas en esta sesión.') + '<a class="btn btn-primary mt-3" href="#practicas">Ir a prácticas</a>'; return; }
  const correctas = resultados.filter(intento => intento.correcta).length;
  main.innerHTML += `<div class="row g-3 mb-4"><div class="col-sm-6"><div class="card card-body"><span>Intentos registrados</span><strong class="fs-2">${resultados.length}</strong></div></div><div class="col-sm-6"><div class="card card-body"><span>Respuestas correctas</span><strong class="fs-2">${correctas}</strong></div></div></div>` + resultados.map(intento => `<article class="card mb-3"><div class="card-body"><span class="badge text-bg-${intento.correcta ? 'success' : 'warning'}">${intento.correcta ? 'Correcta' : 'Por mejorar'}</span><h2 class="h6 mt-3 question-text">${escapar(intento.enunciado)}</h2><p class="mb-0 user-text">Tu respuesta: ${escapar(intento.respuesta)}</p></div></article>`).join('');
}
