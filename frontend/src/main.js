import 'bootstrap/dist/css/bootstrap.min.css';
import { Collapse } from 'bootstrap';
import './styles.css';
import { practicas, verResultados } from './practicas.js';
import { dudas } from './dudas.js';
import { sesion } from './sesion.js';

const main = document.querySelector('main');

/** Renderiza el inicio con accesos reales y sin estadísticas inventadas. */
function inicio() {
  main.innerHTML = `<div class="d-flex justify-content-between flex-wrap gap-2 mb-4 section-label"><span>TU ESPACIO DE APRENDIZAJE</span><span>Un nuevo paso hacia tus metas</span></div>
    <section class="hero mb-5"><div class="row align-items-center g-5"><div class="col-lg-8"><div class="eyebrow mb-3">Aprende a tu ritmo</div><h1>Tu próximo logro<br>empieza <em>aquí.</em></h1><p class="my-4">Practica lo que aprendes, resuelve tus dudas y descubre de lo que eres capaz. Construye tu camino, una pregunta a la vez.</p><a class="btn btn-lime" href="#practicas">Empezar a practicar <span aria-hidden="true">↗</span></a></div><div class="col-lg-4 d-none d-lg-block" aria-hidden="true"><div class="arch"><div class="arch-label">Un paso más cerca<br><strong>de tu mejor versión.</strong></div></div></div></div></section>
    <section aria-labelledby="dashboard"><div class="d-flex align-items-end justify-content-between mb-4"><div><div class="eyebrow text-secondary mb-2">Tu dashboard</div><h2 id="dashboard" class="h3">¿Qué quieres aprender hoy?</h2></div><span class="section-label d-none d-md-inline">Pequeños pasos, grandes avances.</span></div><div class="row g-4">
      ${tarjeta('01', 'Prácticas académicas', 'Encuentra preguntas por tema y dificultad. Pon a prueba tus conocimientos.', 'practicas', 'Explorar preguntas')}
      ${tarjeta('02', 'Un espacio para tus dudas', 'Consulta a tu docente y revisa el estado de tus dudas académicas.', 'dudas', 'Ir a mis dudas')}
      ${tarjeta('03', 'Cada intento cuenta', 'Revisa los resultados confirmados por el backend durante esta sesión.', 'resultados', 'Ver resultados')}
    </div></section><aside class="mt-4 p-4 rounded-3 border bg-white"><strong>Aprender también es preguntar.</strong><p class="text-secondary mb-0 mt-2">El catálogo de prácticas es público. Para enviar respuestas y dudas necesitas un token de alumno válido.</p></aside>`;
}

/** Componente de tarjeta con navegación por enlaces accesibles. */
function tarjeta(numero, titulo, texto, ruta, accion) {
  return `<div class="col-md-6 col-lg-4"><article class="card h-100"><div class="card-body d-flex flex-column"><span class="module-icon" aria-hidden="true">${numero}</span><h3 class="h5">${titulo}</h3><p class="text-secondary mt-2 mb-4">${texto}</p><a class="mt-auto text-success fw-semibold" href="#${ruta}">${accion} →</a></div></article></div>`;
}

/** Navega sin recargar, marca la página actual y cierra el menú móvil. */
function navegar() {
  const ruta = location.hash.slice(1) || 'inicio';
  document.querySelectorAll('.nav-link').forEach(link => {
    const actual = link.hash === `#${ruta}`;
    link.classList.toggle('active', actual);
    if (actual) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
  });
  Collapse.getInstance(document.querySelector('#menu'))?.hide();
  if (ruta === 'inicio') inicio();
  else if (ruta === 'practicas') practicas(main);
  else if (ruta === 'dudas') dudas(main);
  else if (ruta === 'resultados') verResultados(main);
  else if (ruta === 'sesion') sesion(main);
  else main.innerHTML = '<h1 class="page-title">Página no encontrada</h1><a href="#inicio">Volver al inicio</a>';
  document.title = `UMBRAL · ${ruta === 'inicio' ? 'Espacio académico' : main.querySelector('h1').textContent}`;
  main.focus({ preventScroll: true });
  window.scrollTo({ top: 0, behavior: 'instant' });
}
window.addEventListener('hashchange', navegar);
navegar();
