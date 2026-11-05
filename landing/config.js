// Direcciones de cada acceso. Es lo único que hay que tocar para conectar la
// portada con las demás piezas del proyecto: si una dirección queda vacía, la
// puerta se muestra como "Próximamente".
window.UMBRAL = {
  entradas: {
    // Panel de administración (admin/frontend). API Django en http://127.0.0.1:8000
    admin: 'http://localhost:5173',
    // Portal del apoderado (admin/portal-apoderado)
    apoderado: 'http://localhost:5174',
    // Campus web de docentes y alumnos (rama `web`, carpeta frontend/).
    // Se levanta con: npm run dev -- --port 5175
    campus: 'http://127.0.0.1:5175',
    // App móvil Android. Cuando exista un APK o una página de descarga, pegar aquí el enlace.
    movil: '',
  },
}
