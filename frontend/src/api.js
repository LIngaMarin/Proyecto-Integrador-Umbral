import { escapar } from './componentes.js';

// El token vive durante esta pestaña; el cierre de sesión borra la credencial.
export const session = {
  get: () => sessionStorage.getItem('umbral.access') || '',
  set: token => sessionStorage.setItem('umbral.access', token.trim()),
  clear: () => sessionStorage.removeItem('umbral.access'),
};

/** Centraliza errores HTTP; un rechazo del servidor nunca se presenta como éxito. */
export async function api(path, { method = 'GET', body, signal = AbortSignal.timeout(15000), blob = false, protegido = false } = {}) {
  if (protegido && !session.get()) throw new Error('Ingresa con tu acceso de alumno para continuar.');
  const headers = { Accept: blob ? 'application/pdf' : 'application/json' };
  if (session.get()) headers.Authorization = `Bearer ${session.get()}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  let response;
  try {
    response = await fetch(`/api${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), signal, cache: 'no-store' });
  } catch (error) {
    throw new Error(error.name === 'TimeoutError' ? 'El servidor tardó demasiado. Inténtalo de nuevo.' : 'No se pudo conectar con el servicio académico. Inténtalo de nuevo.');
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    if (response.status === 401) {
      session.clear();
      window.dispatchEvent(new Event('session-expired'));
    }
    throw new Error(error.detail || error.message || ({ 401: 'La sesión no es válida o ha vencido.', 403: 'No tienes permiso para esta operación.', 404: 'El contenido solicitado todavía no está disponible.', 409: 'El registro ha cambiado. Actualiza e inténtalo de nuevo.', 503: 'El servicio académico no está disponible.' }[response.status]) || `No se pudo completar la solicitud (${response.status}).`);
  }
  return blob ? response.blob() : response.status === 204 ? null : response.json();
}

/** Escapa texto remoto antes de incluirlo en HTML para impedir ejecución de marcado del usuario. */
export const esc = escapar;
export const query = values => new URLSearchParams(Object.entries(values).filter(([, v]) => v !== '' && v != null)).toString();

// Mantiene los contratos de los módulos de alumno incorporados desde origin/web.
export const guardarToken = value => value ? session.set(value) : session.clear();
export const tieneToken = () => Boolean(session.get());
export const solicitar = api;
Object.assign(api, {
  preguntas: (tema, dificultad) => api(`/practicas/preguntas?${query({ tema, dificultad })}`),
  responder: body => api('/practicas/respuestas', { method: 'POST', body, protegido: true }),
  solucion: id => api(`/practicas/preguntas/${id}/solucionario`, { protegido: true }),
  dudas: estado => api(`/alumno/dudas?${query({ estado })}`, { protegido: true }),
  crearDuda: body => api('/alumno/dudas', { method: 'POST', body, protegido: true }),
  respuestaDuda: id => api(`/alumno/dudas/${id}/respuesta`, { protegido: true }),
  resolverDuda: id => api(`/alumno/dudas/${id}/resolver`, { method: 'PUT', protegido: true }),
});
