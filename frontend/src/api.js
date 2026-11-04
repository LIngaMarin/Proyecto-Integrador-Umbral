// El token vive durante esta pestaña; el cierre de sesión borra la credencial.
export const session = {
  get: () => sessionStorage.getItem('umbral.access') || '',
  set: token => sessionStorage.setItem('umbral.access', token.trim()),
  clear: () => sessionStorage.removeItem('umbral.access'),
};

/** Centraliza errores HTTP; un rechazo del servidor nunca se presenta como éxito. */
export async function api(path, { method = 'GET', body, signal, blob = false } = {}) {
  const headers = { Accept: blob ? 'application/pdf' : 'application/json' };
  if (session.get()) headers.Authorization = `Bearer ${session.get()}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const response = await fetch(`/api${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body), signal });
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
export const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
export const query = values => new URLSearchParams(Object.entries(values).filter(([, v]) => v !== '' && v != null)).toString();
