# UMBRAL — cierre y verificación del Sprint 3

Estado comprobado el 04/11/2026 sobre la rama `web`, partiendo de `5d84119`. Se continuó la aplicación existente: no se reconstruyó, no se eliminaron rutas ni se cambiaron autenticación o relaciones académicas.

## A. Historias y sus cuatro tareas

| Historia | Tarea 1 | Tarea 2 | Tarea 3 | Tarea 4 | Estado real |
|---|---|---|---|---|---|
| HU31 | Agrupación por pregunta o tema normalizado dentro de docente/sección. No existe integración IA configurada. | GET de grupos y detalle autorizados. | Centro docente con temas, cantidades, consultas y filtros. | Normalización, permisos y recorrido integral aprobados. | Operativa por reglas; IA semántica pendiente, sin simularla. |
| HU32 | Endpoint de respuesta agrupada existente. | Una respuesta persistida y vinculada a todas las dudas pendientes sin respuesta. | Formulario e historial docente; lectura propia del alumno. | Prueba nueva con dos alumnos distintos, privacidad y rechazo de reenvío, más regresión existente. | Verificada. Las dudas posteriores necesitan otra respuesta. |
| HU33 | Modelo/tabla con estado PENDIENTE o RESUELTA. | PUT autorizado y resolución idempotente después de una respuesta. | Botón, filtro e historial en ambos paneles. | Estado persistido, permisos, filtro e idempotencia comprobados por HTTP y navegador. | Verificada. |
| HU36 | Cierre programado de clases del portal y detección de alumnos sin presencia. | Aviso interno persistido por alumno/apoderado vinculado. | Notificaciones del apoderado con fecha y detalle. | Cierre automático, exclusión de presentes, unicidad y privacidad comprobados. | Verificada para clases del portal; sin envío externo. |
| HU41 | Plantilla académica A4 con identidad, sección, docente, fecha Lima, indicadores y tablas paginadas. | Consulta JSON y exportación PDF seguras sobre resultados reales. | Reportes académicos con sección autorizada, filtro de simulacro, consulta y Exportar PDF. | Texto, renderizado, cabeceras, filtros, roles, sección inexistente, ausencia de datos, errores y descarga de navegador. | Implementada y verificada. |

No se crearon commits vacíos para HU31, HU33 o HU36: se conservaron y probaron sus implementaciones anteriores. HU31 no se declara agrupación inteligente: no hay cliente, proveedor ni credencial IA disponibles. El usuario pidió documentar esa situación cuando faltara la integración.

## B. Acceso y credenciales demo

El acceso sigue siendo con usuario/correo y contraseña. El backend verifica los hashes BCrypt-SHA256 de los fixtures, el usuario activo y el rol, y emite JWT internamente. No se muestran tokens al usuario ni se guardan contraseñas en el cliente.

| Rol | Usuario | Correo alternativo | Contraseña | Estado |
|---|---|---|---|---|
| Alumno | `alumno.demo` | `alumno@example.invalid` | Entregada en la conversación, fuera de Git | Verificada contra el artefacto final y en localhost:5173 |
| Docente | `docente.demo` | `docente@example.invalid` | Entregada en la conversación, fuera de Git | Verificada; consultó y descargó HU41 |
| Apoderado | `apoderado.demo` | `apoderado@example.invalid` | Entregada en la conversación, fuera de Git | Verificada; vínculo exclusivo con alumno 2 |

No se modificaron los hashes ni las contraseñas. `V902` continúa limitada a demo/test. Las pruebas de navegador utilizan contraseñas aleatorias en memoria; la comprobación final utilizó las contraseñas definitivas de exposición, sin esa sustitución. Se conservaron el rechazo de credenciales incorrectas, la sesión durante la pestaña, el cierre de sesión y los permisos por rol.

## C. Pantallas y funcionamiento

- Público: portada preuniversitaria, carrusel de tres imágenes, controles, indicadores, pausa, movimiento reducido, menú móvil y catálogo público.
- Alumno: inicio, prácticas por tema/nivel, respuestas evaluadas, solucionarios autorizados, historial persistido con filtros, calendario de simulacros, dudas, respuestas docentes, historial, resolución y cuenta.
- Docente: centro de consultas, banco y filtros, importación real de PDF, variantes matemáticas, aprobación/rechazo, revisión y aprobación de solucionarios, estadísticas semanales, temas débiles, clases/asistencia y reportes académicos.
- Apoderado: alumnos vinculados, calendario autorizado y avisos internos de inasistencia.
- HU41: las cifras de pantalla y PDF salen del mismo servicio. Promedio, mínimo y máximo son null si no existen notas; no se sustituyen por cero ni se inventa una escala o un umbral de aprobación.
- Si faltan nombres administrativos de un registro histórico, se muestra su identificador, sin inventar personas o simulacros. Las fechas se incluyen solo cuando existen.
- La descarga anterior desde Resultados sigue funcionando; no se duplicó el endpoint PDF.
- Un error de consulta bloquea exportar datos anteriores. Un error de generación no produce éxito, descarga ni archivo falso.

## D. Backend e inventario de integración

Se revisaron los controladores, servicios, repositorios, DTO, modelos, seguridad y migraciones actuales. Hay **37 rutas HTTP** en los controladores. Todas las pertinentes al portal tienen interfaz; los procesos programados no se exponen como funciones públicas. Administración de usuarios/matrículas/cursos sigue perteneciendo a Django y no se inventa un panel administrativo.

HU41 reutiliza `GET /api/docente/secciones/{id}/rendimiento.pdf` y añade `GET /api/docente/secciones/{id}/rendimiento`. Ambas admiten `simulacroId` opcional. No se añaden filtros por fecha de resultado porque ese campo no existe en el modelo.

La consulta devuelve `seccionId`, `seccionNombre`, `docenteEmail`, `generadoEn`, `simulacroId`, `resumen`, `simulacros`, `resultados` y `temas`. Los indicadores son cantidad de resultados/alumnos, promedio, mínimo y máximo. La exportación conserva la denominación del archivo anterior y las propiedades de seguridad del contrato compartido.

La sección se autoriza antes de leer notas o metadatos. Un docente ajeno, alumno o apoderado recibe 403; un visitante recibe 401. Las secciones inexistentes también conservan 403 para no revelar recursos no asignados. Un filtro inválido devuelve 400; notas o temas corruptos, 422; un fallo de generación, 503 sin trazas ni adjunto.

PDF correcto: `Content-Type: application/pdf`, `Content-Disposition: attachment`, longitud real, `Cache-Control: no-store` y `X-Content-Type-Options: nosniff`. Sin resultados se genera un PDF válido que lo indica.

Todas las rutas siguientes llevan prefijo `/api`. La tabla conserva los contratos previos y refleja la ampliación de HU41:

| Método / ruta | Rol y datos | Respuesta | Interfaz y verificación |
|---|---|---|---|
| POST `/auth/login` | Público; JSON `usuario,password` | 200 `accessToken,expiresIn,rol`; 400/401/429 si falla | Login; HTTP y navegador con los tres roles. |
| GET `/cuenta` | Autenticado | 200 identidad, rol, secciones e hijos autorizados | Panel/perfil; HTTP y E2E. |
| GET `/practicas/preguntas` | Público; `tema,dificultad` | 200 preguntas aprobadas sin solución | Prácticas; HTTP, filtros y estado vacío en navegador. |
| POST `/practicas/respuestas` | Alumno; JSON `preguntaId,respuesta` | 201 intento con evaluación/fecha | Prácticas; HTTP persistencia/permisos y E2E. |
| GET `/practicas/respuestas` | Alumno propio | 200 intentos propios con enunciado, tema, respuesta y evaluación | Resultados persistidos; HTTP privacidad y E2E tras recargar. |
| GET `/practicas/preguntas/{id}/solucionario` | Alumno autorizado e intento previo | 200 solución aprobada; 403/404 si falta requisito | Prácticas; HTTP y E2E. |
| GET `/alumno/simulacros` | Alumno propio; `desde,hasta` opcionales | 200 simulacros activos de sus matrículas | Simulacros y calendario; HTTP y E2E. |
| POST `/alumno/dudas` | Alumno; `docenteId,seccionId,texto,tema` o `preguntaId` | 201 duda agrupada | Mis consultas; HTTP y E2E. |
| GET `/alumno/dudas` | Alumno; `estado` opcional | 200 dudas propias | Mis consultas; HTTP y E2E. |
| GET `/alumno/dudas/{id}/respuesta` | Alumno propietario | 200 respuesta asociada | Mis consultas; privacidad HTTP y E2E. |
| PUT `/alumno/dudas/{id}/resolver` | Alumno propietario; duda respondida | 200 duda resuelta | Mis consultas; HTTP y E2E. |
| GET `/alumno/dudas/{id}/historial` | Alumno propietario | 200 eventos | Mis consultas; HTTP y E2E. |
| GET `/docente/dudas/grupos` | Docente asignado | 200 grupos autorizados | Centro de consultas; HTTP y E2E. |
| GET `/docente/dudas/grupos/{id}` | Docente asignado; `estado` opcional | 200 dudas del grupo | Centro de consultas; HTTP y E2E. |
| POST `/docente/dudas/grupos/{id}/respuestas` | Docente asignado; JSON `texto` | 201 respuesta del lote | Centro; propagación HTTP y E2E. |
| GET `/docente/dudas/grupos/{id}/respuestas` | Docente asignado | 200 respuestas anteriores | Centro; HTTP y E2E. |
| PUT `/docente/dudas/{id}/resolver` | Docente asignado; duda respondida | 200 duda resuelta | Centro; HTTP y E2E. |
| GET `/docente/dudas/{id}/historial` | Docente asignado | 200 eventos | Centro; HTTP y E2E. |
| GET `/docente/preguntas` | Docente; `docenteId` propio, `tema,dificultad` opcionales | 200 preguntas propias | Banco; pruebas de servicio, permisos y E2E. |
| POST `/docente/boletin/extraer` | Docente; multipart `file,docenteId` | 200 preguntas pendientes extraídas del PDF real | Banco; PDFBox y E2E con archivo real. |
| POST `/docente/preguntas/{id}/variantes` | Docente propietario; `docenteId` propio | 200 variante matemática pendiente; 422 si no soporta ecuación | Banco; prueba matemática y E2E. |
| PUT `/docente/preguntas/{id}/aprobar` | Docente propietario; contenido de pregunta | 200 pregunta aprobada | Banco; edición y persistencia E2E. |
| PUT `/docente/preguntas/{id}/rechazar` | Docente propietario; JSON `motivoRechazo` | 200 pregunta rechazada | Banco; servicio y E2E. |
| POST `/docente/preguntas/{id}/solucionario` | Docente propietario | 200 borrador verificado; 422 fuera del patrón soportado | Banco; servicio, HTTP y E2E. |
| GET `/docente/preguntas/{id}/solucionario` | Docente propietario | 200 borrador/aprobado | Banco; revisión E2E. |
| PUT `/docente/solucionarios/{id}/aprobar` | Docente propietario | 200 solucionario aprobado | Banco; HTTP y E2E. |
| GET `/docente/secciones/{seccionId}/temas-debiles` | Docente de la sección | 200 tema y cantidad de errores | Resultados; servicio y E2E. |
| GET `/docente/secciones/{id}/rendimiento` | Docente de la sección; `simulacroId` opcional | 200 metadatos, indicadores, opciones, resultados y temas | Reportes académicos; HTTP, permisos, filtros y E2E. |
| GET `/docente/secciones/{id}/rendimiento.pdf` | Docente de la sección; `simulacroId` opcional | 200 PDF académico A4; attachment, longitud, no-store y nosniff | Reportes académicos y descarga anterior; contenido, renderizado, errores y E2E. |
| GET `/docente/resumen-semanal` | Docente propio; lunes `semana` opcional | 200 estadísticas propias de semana completa | Resultados; HTTP y E2E. |
| GET `/docente/clases` | Docente propio | 200 clases | Clases; HTTP y E2E. |
| POST `/docente/clases` | Docente asignado; `seccionId,titulo,fin` | 201 clase futura | Clases; validación HTTP y E2E. |
| GET `/docente/clases/{id}/alumnos` | Docente propietario | 200 matrícula y presencia | Clases; permisos HTTP y E2E. |
| PUT `/docente/clases/{id}/presentes/{alumnoId}` | Docente; alumno matriculado, clase abierta | 204 | Asistencia; HTTP y E2E. |
| PUT `/docente/clases/{id}/cerrar` | Docente; clase finalizada | 204; 409 antes de finalización | Clases; idempotencia HTTP y mensaje de error E2E. |
| GET `/apoderado/alumnos/{alumnoId}/simulacros` | Apoderado vinculado; `desde,hasta` opcionales | 200 calendario autorizado | Calendario; privacidad HTTP y E2E. |
| GET `/apoderado/alumnos/{alumnoId}/notificaciones` | Apoderado vinculado | 200 avisos reales con alumno, descripción y fecha | Notificaciones; HTTP y proceso completo E2E. |



## E. Código nuevo y modificado

| Archivo | Responsabilidad / comentarios |
|---|---|
| `ReporteRendimientoDTO.kt` | Metadatos, filas e indicadores; documenta null para ausencia de notas. |
| `PlantillaRendimientoPdf.kt` | Plantilla A4, tipografía, colores, tablas, continuidad de celdas, paginación y texto seguro. |
| `RendimientoPdfService.kt` | Autorización, consultas parametrizadas, filtro, validación de datos, cálculos y manejo de errores. |
| `RendimientoPdfController.kt` | Consulta previa y exportación con permisos y cabeceras. |
| `frontend/src/reportes.js` | Formulario, HTML comentado, consulta real, estados de carga/error, descarga y prevención de respuestas antiguas. |
| `frontend/src/main.js` | Navegación, acceso rápido y vinculación del panel de reportes. |
| `frontend/src/style.css` | Indicadores, tablas y reglas responsive de HU41 comentadas en español. |
| `PlantillaRendimientoPdfTests.kt` | Contenido, documento vacío y tablas extensas renderizadas. |
| `ReporteRendimientoHttpTests.kt` | Datos persistidos, filtro, cabeceras, permisos, ausencia de datos y datos inválidos. |
| `FalloReportePdfHttpTests.kt` | Fallo del generador forzado solo en pruebas, con Accept PDF y respuesta 503 segura. |
| `frontend/tests/reportes.spec.js` | Seis escenarios de HU41 con descarga, filtros, errores, roles y móvil. |
| `frontend/tests/portal.e2e.mjs` | Recorrido integral ampliado con HU41 y artefacto de backend configurable. |
| `RespuestasHttpTests.kt` | Prueba adicional de HU32 con dos alumnos y sin duplicar respuestas. |
| `backend/pom.xml` | Directorio de compilación configurable para no sobrescribir un JAR abierto. |
| `frontend/playwright.config.js`, `scripts-demo.mjs` | Selección del artefacto aislado sin cambiar las rutas predeterminadas. |

El código nuevo o modificado incluye comentarios útiles en español; no se comentaron líneas triviales ni se cambiaron clases ajenas solo para documentarlas. No hicieron falta nuevas entidades ni migraciones para HU41. Se reutiliza PDFBox, sin dependencias nuevas.

## F. Verificación ejecutada

- Base anterior: 62 pruebas aprobadas. El primer empaquetado detectó un JAR abierto por otra instancia de Windows; se corrigió con un directorio de compilación configurable.
- Final: **72 pruebas del backend aprobadas**, compilación y empaquetado correctos.
- Cliente HTTP: **3 pruebas aprobadas**.
- Navegador: **24 escenarios aprobados**, incluyendo seis nuevos de HU41 y los dieciocho anteriores de login, roles, carrusel, errores y responsive.
- Recorrido integral: tres perfiles, prácticas, soluciones, historial persistido, calendario, HU31, HU32, HU33, importación, variantes, estadísticas, PDF anterior, HU41, asistencia y aviso real de HU36.
- Credenciales definitivas: tres logins y llamadas autenticadas correctas contra demo, sin contraseñas efímeras de pruebas.
- Campus visible: login de los tres perfiles, consulta HU41 y PDF descargado desde `http://localhost:5173`, sin errores de consola.
- PDF: apertura y extracción con PDFBox, renderizado de primera y última página, metadatos y tablas largas. Se revisaron visualmente el documento y las pantallas de escritorio/móvil.
- Instalación `npm ci`, compilación Vite y pruebas del cliente correctas. Fue necesario detener los procesos de Vite de este workspace para liberar esbuild en Windows; los backends anteriores no se detuvieron.

Los fallos forzados de generación o respuestas HTTP existen solo en pruebas. No hay respuestas de respaldo ni datos fijos nuevos en el código de producción.

Evidencia local ignorada por Git:

- `backend/target/sprint3-build.log` y `backend/target/sprint3/surefire-reports/`.
- `backend/target/hu41-reporte-verificado.pdf` y su imagen: fixture aislado de las pruebas HTTP.
- `frontend/test-results/hu41-flujo.pdf`: descarga del recorrido integral demo.
- `frontend/test-results/hu41-presentacion.pdf`: descarga del campus final.
- `frontend/test-results/hu41-reportes.png`, `hu41-reportes-movil.png` y `hu41-campus-final.png`.

## G. Ejecución desde C:\UMBRAL

El campus quedó preparado en **http://localhost:5173/** con backend nuevo en **8082**. Los backends anteriores en 8080/8081 se preservaron. Se usa una copia de ejecución distinta del JAR de compilación para que otra compilación no quede bloqueada.

Comandos para preparar y arrancar de nuevo, con los procesos de esa nueva instancia detenidos:

```powershell
$env:JAVA_HOME='C:\Users\sheil\.jdks\openjdk-26.0.2.1'
cd C:\UMBRAL\backend
.\mvnw.cmd -o -B -Pdemo '-Dumbral.build.directory=target/sprint3' package
New-Item -ItemType Directory -Force target/runtime-sprint3 | Out-Null
Copy-Item -LiteralPath target/sprint3/umbral-backend-0.0.1-SNAPSHOT.jar -Destination target/runtime-sprint3/umbral-backend.jar
cd ..\frontend
npm.cmd ci
$env:UMBRAL_BACKEND_JAR='C:\UMBRAL\backend\target\runtime-sprint3\umbral-backend.jar'
$env:UMBRAL_DEMO_PORT='8082'
npm.cmd run demo
```

Segunda terminal:

```powershell
cd C:\UMBRAL\frontend
$env:UMBRAL_API_TARGET='http://127.0.0.1:8082'
npm.cmd run dev
```

Verificación reproducible con artefacto aislado:

```powershell
$env:JAVA_HOME='C:\Users\sheil\.jdks\openjdk-26.0.2.1'
cd C:\UMBRAL\backend
.\mvnw.cmd -o -B -Pdemo '-Dumbral.build.directory=target/sprint3' package
cd ..\frontend
$env:UMBRAL_BACKEND_JAR='C:\UMBRAL\backend\target\sprint3\umbral-backend-0.0.1-SNAPSHOT.jar'
npm.cmd test
npm.cmd run build
npm.cmd run test:browser
npm.cmd run test:e2e
```

Las suites de navegador deben ejecutarse secuencialmente; usan sus propios puertos y H2. No reutilizan el servidor del usuario. Para reinstalar dependencias hay que detener Vite; no es necesario detener las instancias de backend que usan otros artefactos. Node 24, JDK >=25 y Edge están disponibles; `UMBRAL_BROWSER` permite otra ruta Chromium. En otro equipo sin dependencias Maven descargadas, omitir `-o` y configurar certificados válidos, sin desactivar TLS.

PostgreSQL conserva la configuración por entorno y el contrato administrativo anterior. No se ejecutó un servidor PostgreSQL real ni se alteraron datos de producción. H2 demo persiste entre peticiones, pero es efímera al detener la instancia.

## H. Limitaciones y exposición

1. **HU31 semántica con IA sigue pendiente**: falta una integración real disponible y sus credenciales/contrato. La agrupación automática actual funciona por reglas; no se renombró como IA ni se crearon llamadas simuladas.
2. HU36 envía avisos internos, no correos, SMS o push. Solo cubre clases registradas en el portal; la aplicación detenida puede retrasar el aviso.
3. Los resultados no tienen fecha propia ni escala formal de aprobación. HU41 filtra por simulacro y reporta puntajes, no porcentajes, aprobados o intervalos de fechas inventados.
4. Un resultado histórico cuyo alumno/simulacro no tenga metadatos administrativos se identifica por su ID. Los fixtures demo anteriores incluyen un resultado del perfil 1 sin perfil ALUMNO correspondiente; se conserva y se muestra por ID, sin inventar identidad ni sobrescribir información.
5. La fuente PDF estándar conserva español; caracteres no disponibles se representan por `?`. No se incluye HTML ni contenido ejecutable en el PDF.
6. PostgreSQL real, IA y entrega externa de notificaciones requieren verificaciones independientes. El calendario no se presenta como evaluación resoluble.
7. Se conservaron los cambios locales previos en `application.properties`, `package-lock.json`, notas, PDF, ZIP y `.run`, fuera de los commits nuevos.

Guion de exposición: alumno registra dos dudas de un tema; docente abre el grupo y responde; cada alumno consulta su respuesta; se marca una duda respondida como resuelta y se comprueba su historial. Para HU36, una clase finalizada sin presencia genera un aviso del alumno vinculado. Para HU41, docente abre Reportes académicos, selecciona sección/simulacro, consulta los datos y pulsa Exportar PDF; el archivo contiene esa selección y los indicadores reales.

## I. Git

Se hicieron commits reales por tarea de HU41 y uno separado de pruebas para HU32. La corrección de compilación aislada se registró aparte, sin atribuirla a una historia académica. No se cambió de rama, no se reescribió historial y no se incorporaron contraseñas, tokens, ZIP, capturas ni archivos temporales. Los identificadores y la sincronización definitiva se informan en la conversación.

| Commit | Historia o alcance |
|---|---|
| `f583572` | HU41, tarea 1: plantilla A4, DTO y pruebas de contenido/paginación. |
| `e4930fa` | HU41, tarea 2: consulta previa, filtro y exportación autorizada. |
| `533c58c` | HU41, tarea 3: Reportes académicos y descarga desde el panel docente. |
| `27079e7` | HU41, tarea 4: pruebas HTTP, generación fallida, descarga y navegador. |
| `3bcd8d1` | HU32: prueba de propagación real entre alumnos y rechazo de duplicados. |
| `ff06185` | Compilación/ejecución: artefactos aislados para evitar bloqueos de Windows. |

La documentación de cierre se registra en otro commit; su propio identificador se entrega en la conversación.
