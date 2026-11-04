# UMBRAL · Portal académico

Portal JavaScript, Vite 7 y Bootstrap 5 conectado al backend Kotlin. Incluye catálogo público y vistas de alumno, docente y apoderado con JWT. La entrega amplía los seis commits previos de origin/web mediante merge normal; conserva sus módulos, validación compartida, resultados temporales y contratos API.

La guía completa y actualizada de funcionalidades, endpoints, accesos demo, pruebas, archivos y limitaciones está en [ENTREGA_WEB.md](../ENTREGA_WEB.md).

Primero generar el JAR con Maven -Pdemo y configurar JAVA_HOME con JDK >=25. Dentro de frontend ejecutar npm.cmd ci y npm.cmd run demo. En otra terminal ejecutar npm.cmd run dev. Abrir http://127.0.0.1:5173.

El lanzador demo muestra tres access tokens de prueba en su consola, válidos solo contra esa instancia local durante dos horas. El portal valida el token contra /api/cuenta; los roles y vínculos proceden de la base. La sesión dura la pestaña (sessionStorage); cerrar sesión elimina token y resultados temporales. El catálogo también puede explorarse sin autenticación.

Pruebas: npm.cmd test (cliente HTTP), npm.cmd run test:browser (11 escenarios de regresión adaptados), npm.cmd run test:e2e (flujo integral de tres perfiles) y npm.cmd run build. Las pruebas requieren el JAR H2 y Edge o UMBRAL_BROWSER con ruta Chromium. Inician servidores aislados; no reutilizan la aplicación del usuario. No se guardan tokens ni claves en archivos.

src/componentes.js mantiene validación y componentes compartidos. src/practicas.js conserva resultados confirmados de sesión; los módulos remotos siguen teniendo API compatible. src/main.js añade navegación por roles, consultas docentes, clases/asistencia, reportes y apoderado. src/api.js centraliza JWT, timeout, errores y compatibilidad. src/style.css define la apariencia actual; src/styles.css conserva estilos anteriores de componentes heredados.

La compilación genera dist. Desarrollo y preview usan proxy /api a 8080; producción requiere configurar ese proxy en el servidor HTTPS. No se incluye login ficticio ni se desactiva seguridad.
