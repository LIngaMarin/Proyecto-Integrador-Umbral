/** Escapa datos externos antes de incorporarlos al HTML de las tarjetas. */
export function escapar(valor) {
  return String(valor ?? '').replace(/[&<>"']/g, caracter => ({ '&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;' })[caracter]);
}

/** Encabezado y avisos compartidos por las páginas. */
export const encabezado = (titulo, descripcion) => `<div class="eyebrow text-secondary mb-2">Tu espacio académico</div><h1 class="page-title">${titulo}</h1><p class="text-secondary mb-4">${descripcion}</p>`;
export const vacio = texto => `<div class="empty-state"><p class="mb-0">${escapar(texto)}</p></div>`;
export function aviso(elemento, texto, tipo = 'danger') {
  elemento.innerHTML = `<div class="alert alert-${tipo}" role="${tipo === 'danger' ? 'alert' : 'status'}">${escapar(texto)}</div>`;
}

/** Deshabilita el envío durante la petición y siempre restablece el botón. */
export async function tarea(boton, destino, accion) {
  const contenido = boton.innerHTML;
  boton.disabled = true;
  boton.innerHTML = '<span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Procesando…';
  destino.setAttribute('aria-busy', 'true');
  destino.replaceChildren();
  try { await accion(); }
  catch (error) { aviso(destino, error.message); }
  finally { boton.disabled = false; boton.innerHTML = contenido; destino.removeAttribute('aria-busy'); }
}

/** Aplica restricciones HTML y evita enviar texto compuesto solo de espacios. */
export function validar(formulario) {
  formulario.querySelectorAll('input[type="text"], textarea').forEach(campo => {
    campo.setCustomValidity(campo.required && !campo.value.trim() ? 'Escribe un texto válido.' : '');
    campo.oninput = () => campo.setCustomValidity('');
  });
  return formulario.reportValidity();
}
