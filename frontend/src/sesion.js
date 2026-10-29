import { guardarToken, tieneToken } from './api.js';
import { limpiarResultados } from './practicas.js';
import { encabezado, aviso } from './componentes.js';

/** Permite usar un token obtenido del proveedor real; no simula inicio de sesión. */
export function sesion(main) {
  main.innerHTML = encabezado('Acceso de alumno', 'Conecta tu identidad académica para enviar respuestas y dudas.') + `
    <section class="card mx-auto" style="max-width: 680px"><div class="card-body"><h2 class="h5">Token de acceso JWT</h2><p class="text-secondary">El backend Kotlin no ofrece un endpoint de login. Obtén tu token de acceso del sistema administrativo autorizado. Guardarlo aquí no confirma su validez: Spring Boot verificará firma, vencimiento y permisos en cada petición.</p><form id="acceso"><label class="form-label" for="token">Token de alumno</label><input class="form-control" id="token" name="token" type="password" autocomplete="off" spellcheck="false" required aria-describedby="token-ayuda"><p id="token-ayuda" class="form-text">Se conserva solo en memoria. Se elimina al recargar o cerrar la página. No se envía en consultas públicas.</p><div class="d-flex flex-wrap gap-2"><button class="btn btn-primary">Usar token</button><button class="btn btn-outline-dark" type="button" id="salir">Eliminar token</button></div></form><div class="mt-3" id="estado-sesion" aria-live="polite"></div></div></section>`;
  const estado = main.querySelector('#estado-sesion');
  aviso(estado, tieneToken() ? 'Hay un token en memoria, pendiente de verificación en el servidor.' : 'No hay un token guardado. Puedes consultar el catálogo público.', 'info');
  main.querySelector('#acceso').addEventListener('submit', event => {
    event.preventDefault();
    const campo = main.querySelector('#token');
    const token = campo.value.trim();
    // Solo comprueba la estructura; la autenticación se delega siempre al backend.
    if (!/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/.test(token)) {
      aviso(estado, 'Introduce un JWT con tres segmentos, sin el prefijo Bearer.'); return;
    }
    guardarToken(token);
    limpiarResultados();
    campo.value = '';
    aviso(estado, 'Token guardado en memoria. Spring Boot lo verificará al consultar una ruta protegida.', 'success');
  });
  main.querySelector('#salir').addEventListener('click', () => {
    guardarToken(''); limpiarResultados(); main.querySelector('#token').value = '';
    aviso(estado, 'Token y resultados de sesión eliminados.', 'info');
  });
}
