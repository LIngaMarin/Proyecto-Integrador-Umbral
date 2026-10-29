import { api } from './api.js';
import { encabezado, escapar, vacio, tarea, validar, aviso } from './componentes.js';

/** Formulario de dudas y consulta de registros propios del alumno autenticado. */
export function dudas(main) {
  main.innerHTML = encabezado('Tus dudas también importan', 'Envía tu consulta al docente asignado y sigue su respuesta.') + `
    <div class="row g-4"><section class="col-lg-5"><div class="card"><div class="card-body"><h2 class="h5">Nueva duda académica</h2><p class="text-secondary small">Necesitas un token de alumno. Solicita a tu academia los identificadores de docente y sección; todavía no existe un catálogo público para seleccionarlos.</p>
    <!-- Identificadores reales: el backend comprueba matrícula y asignación docente. -->
    <form id="nueva-duda"><div class="row g-3"><div class="col-sm-6"><label for="docente" class="form-label">ID del docente</label><input id="docente" name="docenteId" type="number" min="1" max="9007199254740991" step="1" required class="form-control"></div><div class="col-sm-6"><label for="seccion" class="form-label">ID de sección</label><input id="seccion" name="seccionId" type="number" min="1" max="9007199254740991" step="1" required class="form-control"></div><div class="col-12"><label class="form-label" for="duda-tema">Tema</label><input id="duda-tema" name="tema" class="form-control" type="text" maxlength="255" required></div><div class="col-12"><label for="texto" class="form-label">¿Qué necesitas comprender?</label><textarea id="texto" name="texto" class="form-control" maxlength="4000" rows="5" required></textarea><div class="form-text">Máximo 4000 caracteres. No incluyas contraseñas ni tokens.</div></div></div><button class="btn btn-primary mt-3">Enviar duda</button></form><div id="aviso-duda" class="mt-3" aria-live="polite"></div></div></div></section>
    <section class="col-lg-7"><h2 class="h4">Mis consultas</h2><form id="consultar-dudas" class="d-flex flex-wrap gap-2 mb-3"><div class="flex-grow-1"><label for="estado" class="form-label">Estado</label><select id="estado" name="estado" class="form-select"><option value="">Todos</option><option value="PENDIENTE">Pendientes</option><option value="RESUELTA">Resueltas</option></select></div><button class="btn btn-primary align-self-end">Consultar dudas</button></form><div id="lista-dudas" aria-live="polite">${vacio('Pulsa Consultar dudas para cargar tus registros con el token de alumno.')}</div></section></div>`;
  const form = main.querySelector('#nueva-duda');
  const filtros = main.querySelector('#consultar-dudas');
  const lista = main.querySelector('#lista-dudas');
  form.addEventListener('submit', event => {
    event.preventDefault();
    if (!validar(form)) return;
    tarea(form.querySelector('button'), main.querySelector('#aviso-duda'), async () => {
      const campos = new FormData(form);
      const creada = await api.crearDuda({ docenteId: Number(campos.get('docenteId')), seccionId: Number(campos.get('seccionId')), tema: campos.get('tema').trim(), texto: campos.get('texto').trim() });
      aviso(main.querySelector('#aviso-duda'), `Duda #${creada.id} registrada. Consulta la lista para seguir su estado.`, 'success');
      form.reset();
    });
  });
  filtros.addEventListener('submit', event => {
    event.preventDefault();
    tarea(filtros.querySelector('button'), lista, async () => {
      const registros = await api.dudas(filtros.elements.estado.value);
      if (!Array.isArray(registros)) throw new Error('La lista de dudas no tiene el formato esperado.');
      lista.innerHTML = registros.length ? '' : vacio('No hay dudas para el estado seleccionado.');
      registros.forEach(duda => lista.append(crearDuda(duda)));
    });
  });
}

/** Tarjeta que consulta respuestas y solicita resolución sin alterar el estado local por adelantado. */
function crearDuda(duda) {
  const tarjeta = document.createElement('article');
  tarjeta.className = 'card mb-3';
  tarjeta.innerHTML = `<div class="card-body"><div class="d-flex justify-content-between gap-2"><h3 class="h6">Duda #${escapar(duda.id)}</h3><span class="badge text-bg-light">${escapar(duda.estado)}</span></div><p class="user-text mt-3">${escapar(duda.texto)}</p><div class="d-flex flex-wrap gap-2"><button class="btn btn-outline-dark consultar">Ver respuesta</button>${duda.estado !== 'RESUELTA' ? '<button class="btn btn-outline-dark resolver">Marcar como resuelta</button>' : ''}</div><div class="feedback mt-3" aria-live="polite"></div></div>`;
  const feedback = tarjeta.querySelector('.feedback');
  tarjeta.querySelector('.consultar').addEventListener('click', event => tarea(event.currentTarget, feedback, async () => {
    const respuesta = await api.respuestaDuda(duda.id);
    feedback.innerHTML = `<h4 class="h6">Respuesta del docente</h4><p class="user-text">${escapar(respuesta.texto)}</p>`;
  }));
  tarjeta.querySelector('.resolver')?.addEventListener('click', event => {
    const boton = event.currentTarget;
    tarea(boton, feedback, async () => {
      await api.resolverDuda(duda.id);
      tarjeta.querySelector('.badge').textContent = 'RESUELTA';
      boton.remove();
      aviso(feedback, 'Duda resuelta. Actualiza la lista para aplicar de nuevo el filtro.', 'success');
    });
  });
  return tarjeta;
}
