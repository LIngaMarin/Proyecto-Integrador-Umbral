let token = '';

/** Conserva el token solo en memoria; recargar o cerrar la página elimina el acceso. */
export function guardarToken(valor) { token = valor; }
export function tieneToken() { return Boolean(token); }

/** Envía JSON al proxy local, aplica un límite de espera y respeta las rutas con JWT. */
export async function solicitar(ruta, { method = 'GET', body, protegido = false } = {}) {
  if (protegido && !token) throw new Error('Introduce un token de alumno válido en Acceso de alumno.');
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (protegido) headers.Authorization = `Bearer ${token}`;
  let respuesta;
  try {
    respuesta = await fetch(`/api${ruta}`, {
      method, headers, body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(15000), cache: 'no-store',
    });
  } catch (error) {
    throw new Error(error.name === 'TimeoutError' ? 'El servidor tardó demasiado. Vuelve a intentarlo.' : 'No se pudo conectar con Spring Boot. Comprueba que esté iniciado en el puerto 8080.');
  }
  // No expone trazas del servidor ni interpreta mensajes HTTP como HTML.
  if (!respuesta.ok) {
    if (respuesta.status === 401) token = '';
    const mensajes = { 400: 'Revisa los datos enviados.', 401: 'El token no es válido o venció. Vuelve a introducirlo.', 403: 'No tienes permiso académico para esta operación; puede requerir un intento previo.', 404: 'No hay un registro o solución publicada disponible.', 409: 'El estado cambió o la duda aún no tiene respuesta. Actualiza e inténtalo otra vez.' };
    throw new Error(mensajes[respuesta.status] || `No se pudo completar la solicitud (HTTP ${respuesta.status}). Inténtalo más tarde.`);
  }
  if (respuesta.status === 204) return null;
  try { return await respuesta.json(); }
  catch { throw new Error('El servidor no devolvió un JSON válido.'); }
}

/** Contratos observados en los controladores Kotlin; no se inventan rutas de login. */
export const api = {
  preguntas: (tema, dificultad) => solicitar(`/practicas/preguntas?${new URLSearchParams({ tema, dificultad })}`),
  responder: body => solicitar('/practicas/respuestas', { method: 'POST', body, protegido: true }),
  solucion: id => solicitar(`/practicas/preguntas/${id}/solucionario`, { protegido: true }),
  dudas: estado => solicitar(`/alumno/dudas${estado ? `?${new URLSearchParams({ estado })}` : ''}`, { protegido: true }),
  crearDuda: body => solicitar('/alumno/dudas', { method: 'POST', body, protegido: true }),
  respuestaDuda: id => solicitar(`/alumno/dudas/${id}/respuesta`, { protegido: true }),
  resolverDuda: id => solicitar(`/alumno/dudas/${id}/resolver`, { method: 'PUT', protegido: true }),
};
