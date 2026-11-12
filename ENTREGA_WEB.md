# UMBRAL — entrega del portal académico

> Cierre vigente del Sprint 3: [CIERRE_SPRINT3_WEB.md](CIERRE_SPRINT3_WEB.md), con HU41, inventario de 37 rutas, pruebas y comandos actuales. Las secciones siguientes conservan el historial de la primera entrega.

> Documento histórico del 03/11/2026. El acceso actual es con usuario y contraseña; consultar [ENTREGA_LOGIN_Y_DISENO.md](ENTREGA_LOGIN_Y_DISENO.md) para ejecución, seguridad, endpoints y funciones actualizadas. Ya no se introducen tokens manualmente y el historial de prácticas ahora es persistido.

## Estado y alcance

Se trabajó sobre la rama `web`, partiendo de `dda3167`, con frontend vacío y backend Kotlin existente. Se conservan JWT, PostgreSQL, H2 demo, contratos móviles y migraciones anteriores. Los archivos que ya estaban sin seguimiento (`.run/`, `SITES10.pdf`, `backend.zip`, `backend/CONEXION_LOCAL.md`, `backend/DEMO_LOCAL.md`) no forman parte de la entrega.

Al consultar GitHub se encontraron seis commits remotos todavía ausentes de esta copia, desde `bb88f67` hasta `8fd5f8f`. Se incorporaron mediante merge normal, sin cambiar de rama ni reescribir su historial. Se preservaron módulos, catálogo público, resultados confirmados de sesión, validación compartida, proxy de preview y Vite 7. Sus once escenarios de navegador se adaptaron a las vistas por rol y se volvieron a ejecutar.

| Historia solicitada | Implementación | Evidencia | Estado |
|---|---|---|---|
| HU31 | Centro docente, temas, recuentos reales, preguntas relacionadas, búsqueda de tema y filtro de estado. Reutiliza agrupación por pregunta/tema normalizado y sección/docente. | `DudaUnitTests`, `RespuestasHttpTests`, prueba de navegador con dos consultas en un grupo. | Funciona agrupación determinista. **IA semántica pendiente**: no existe proveedor, cliente ni credencial IA en el repositorio. |
| HU32 | Respuesta única persistida y asociada transaccionalmente a todas las dudas pendientes sin respuesta del grupo. Alumno consulta su respuesta; docente revisa historial. | Pruebas HTTP de propagación/permisos existentes y flujo real en navegador. | Implementada. Las consultas posteriores requieren nueva respuesta; se conserva la política existente. |
| HU33 | Resolución por docente asignado o alumno propietario; requiere respuesta previa, persiste estado e historial, admite repetición sin duplicar evento. | Pruebas HTTP de resolución y filtro; navegador verifica lectura posterior e historial. | Implementada. |
| HU36 | Clases del portal, asistencia docente, detección automática al finalizar, notificaciones internas por hijo vinculado y lectura en portal. | HTTP prueba cierre programado, persistencia, exclusión de presentes, idempotencia y rechazo de acceso ajeno. | Implementada para **clases registradas en el portal** y avisos internos. Sin SMS, correo ni notificación push de inasistencia. |

El contrato anterior numera agrupación/respuesta/resolución como HU28/29/30. Esta entrega las relaciona con HU31/32/33 del encargo sin renombrar ni romper sus rutas.

## Funciones por perfil

- Alumno: inicio con recuentos propios; búsqueda de prácticas por tema/dificultad; resolución, envío y evaluación persistida; solucionario aprobado tras intento; nueva consulta con sección/docente autorizados; consultas propias filtrables, respuesta, historial y resolución; cuenta y cierre de sesión.
- Docente: inicio con grupos/secciones; centro de consultas agrupadas, filtros, detalle, respuesta por lote, historial y resolución; programación de clases, lista de matrícula, registro de presentes y cierre de asistencia; banco con filtros, edición/aprobación, rechazo motivado; generación de solucionario verificable, consulta de borrador y aprobación; resumen semanal, temas débiles y PDF por sección; cuenta y cierre de sesión.
- Apoderado: inicio con número real de hijos vinculados, calendario de simulacros por alumno/rango, notificaciones internas de inasistencia, cuenta y cierre de sesión.
- Administrador: este backend no implementa API administrativa. La administración y matrícula pertenecen al servicio Django compartido; no se inventa un panel ni se concede un rol docente a administradores.
- Visitante: catálogo público con filtros desde «Explorar prácticas públicas», sin poder registrar respuestas o consultas.
- Resultados del alumno: se conservan los intentos confirmados de la sesión, heredados del frontend remoto; no se inventa un historial global.

## Inventario completo de rutas Kotlin

Todas las rutas siguientes llevan prefijo `/api`. JWT salvo catálogo público. Los identificadores referencian perfiles académicos, salvo `user_id` del JWT que referencia `auth_user`. Los servicios comprueban propiedad, matrícula, sección o vínculo según corresponda. `HTTP` refiere a la suite Spring con servidor real y H2; `E2E` a Edge con el backend demo real. Los estados de implementación indican disponibilidad en este portal; el inventario describe también las rutas que ya existían.

| Método y ruta | Función/rol autorizado | Datos requeridos | Respuesta correcta | Pantalla / estado | Prueba |
|---|---|---|---|---|---|
| GET `/cuenta` | Identidad y relaciones, alumno/docente/apoderado | JWT | 200 `{id,rol,email,secciones,hijos}` | Todas, conectada, nueva | HTTP tres roles, E2E |
| GET `/practicas/preguntas` | Catálogo público aprobado | query `tema,dificultad` | 200 preguntas sin solución | Prácticas, conectada | HTTP, E2E |
| POST `/practicas/respuestas` | Registrar intento, alumno | JSON `preguntaId,respuesta` | 201 intento con `correcta,fecha` | Prácticas, conectada | HTTP persistencia/permisos, E2E |
| GET `/practicas/preguntas/{id}/solucionario` | Solución aprobada tras intento, alumno | id pregunta | 200 `preguntaId,respuestaCorrecta,pasoAPaso,explicacionErroresComunes` | Prácticas, conectada | HTTP 403/404/200, E2E |
| POST `/alumno/dudas` | Registrar duda, alumno matriculado | JSON `docenteId,seccionId,texto` y `tema` o `preguntaId` | 201 duda | Mis consultas por tema, conectada | HTTP validación/agrupación, E2E |
| GET `/alumno/dudas` | Listar dudas propias, alumno | query opcional `estado` | 200 dudas | Mis consultas, conectada | HTTP, E2E |
| GET `/alumno/dudas/{id}/respuesta` | Respuesta a duda propia, alumno | id duda | 200 respuesta compartida o 404 si falta | Mis consultas, conectada | HTTP privacidad, E2E |
| PUT `/alumno/dudas/{id}/resolver` | Resolver duda propia, alumno | id duda respondida | 200 duda resuelta | Mis consultas, conectada | HTTP idempotencia, E2E |
| GET `/alumno/dudas/{id}/historial` | Historial propio, alumno | id duda | 200 eventos con actor/fecha | Mis consultas, conectada | HTTP, E2E |
| GET `/docente/dudas/grupos` | Grupos de secciones asignadas, docente | JWT | 200 grupos | Centro de consultas, conectada | HTTP, E2E |
| GET `/docente/dudas/grupos/{id}` | Dudas del grupo propio, docente | id grupo; `estado` opcional | 200 dudas | Centro, conectada (filtro local sobre lectura autorizada) | HTTP, E2E |
| POST `/docente/dudas/grupos/{id}/respuestas` | Responder lote, docente asignado | JSON `texto` máximo 10000 | 201 respuesta persistida | Centro, conectada | HTTP propagación, E2E |
| GET `/docente/dudas/grupos/{id}/respuestas` | Historial de respuestas, docente asignado | id grupo | 200 respuestas | Centro, conectada | HTTP; control implementado |
| PUT `/docente/dudas/{id}/resolver` | Resolver duda de sección propia, docente | id duda respondida | 200 duda | Centro, conectada | HTTP; control implementado |
| GET `/docente/dudas/{id}/historial` | Historial autorizado, docente | id duda | 200 eventos | Centro, conectada | HTTP; control implementado |
| GET `/docente/preguntas` | Banco propio, docente | `docenteId` propio; `tema,dificultad` opcionales | 200 preguntas completas | Banco, conectada | Pruebas servicio, HTTP permisos, E2E |
| POST `/docente/boletin/extraer` | Extracción, docente | multipart `file,docenteId` propio | 200 lista | **No expuesta**: servicio existente genera una pregunta fija, no extrae archivo | Servicio inspeccionado; no afirmado como extracción real |
| POST `/docente/preguntas/{id}/variantes` | Variante de pregunta propia, docente | id pregunta, query `docenteId` propio | 200 pregunta | **No expuesta**: servicio existente genera texto fijo, sin IA | Servicio inspeccionado; dependencia IA pendiente |
| PUT `/docente/preguntas/{id}/aprobar` | Editar/aprobar pregunta propia, docente | JSON `enunciado,tema,dificultad,opciones,respuestaCorrecta` | 200 pregunta aprobada | Banco, conectada | Pruebas existentes; control implementado |
| PUT `/docente/preguntas/{id}/rechazar` | Rechazar pregunta propia, docente | JSON `motivoRechazo` | 200 pregunta rechazada | Banco, conectada | Pruebas servicio; control implementado |
| POST `/docente/preguntas/{id}/solucionario` | Solución por regla matemática verificable, docente propietario | id pregunta | 200 borrador o 422 si ecuación no soportada | Banco, conectada | Pruebas servicio, HTTP, E2E |
| GET `/docente/preguntas/{id}/solucionario` | Leer borrador/aprobado propio, docente propietario | id pregunta | 200 solucionario o 404 | Banco, conectada, nueva | E2E consulta real |
| PUT `/docente/solucionarios/{id}/aprobar` | Publicar borrador propio, docente propietario | id solucionario | 200 aprobado | Banco, conectada tras revisión | HTTP, E2E |
| GET `/docente/secciones/{seccionId}/temas-debiles` | Errores agregados de sección asignada, docente | id sección | 200 `{tema,cantidadErrores}[]` | Resultados, conectada | Pruebas servicio, E2E |
| GET `/docente/secciones/{id}/rendimiento.pdf` | Informe real sección propia, docente | id sección | 200 PDF, sin caché | Resultados, conectada | HTTP y pruebas PDFBox, E2E descarga |
| GET `/docente/resumen-semanal` | Estadísticas propias, docente | `semana` lunes de semana completa, opcional | 200 estadísticas con período | Resultados, conectada | HTTP, E2E |
| GET `/apoderado/alumnos/{alumnoId}/simulacros` | Calendario del hijo vinculado, apoderado | id alumno; `desde,hasta` opcionales | 200 `{id,nombre,fecha}[]` | Calendario, conectada | HTTP permisos/rangos, E2E |
| GET `/docente/clases` | Clases propias, docente | JWT | 200 clases | Clases, conectada, nueva | HTTP/E2E |
| POST `/docente/clases` | Programar clase propia, docente | JSON `seccionId,titulo,fin` (fecha/hora Lima) | 201 clase | Clases, conectada, nueva | HTTP validación/permisos, E2E |
| GET `/docente/clases/{id}/alumnos` | Matrícula/presencia sección propia, docente | id clase | 200 `{id,email,presente}[]` | Clases, conectada, nueva | HTTP privacidad, E2E |
| PUT `/docente/clases/{id}/presentes/{alumnoId}` | Presencia en clase abierta, docente | ids clase/alumno matriculado | 204 | Clases, conectada, nueva | HTTP idempotencia/permisos, E2E |
| PUT `/docente/clases/{id}/cerrar` | Cierre de clase finalizada, docente | id clase | 204 o 409 si aún no finalizó | Clases, conectada, nueva | HTTP fecha/idempotencia |
| GET `/apoderado/alumnos/{alumnoId}/notificaciones` | Avisos internos del hijo vinculado, apoderado | id alumno | 200 avisos con alumno, descripción y fecha | Notificaciones, conectada, nueva | HTTP vínculo/persistencia, E2E |

No hay endpoints exclusivamente móviles en estos controladores; los contratos existentes continúan utilizables por el cliente móvil con sus permisos. Los DAO Room móviles no son APIs HTTP. No existen rutas de login con contraseña, notificaciones push, consulta de resultados individuales por alumno/apoderado, gestión de usuarios o envío de simulacros en este servicio. Por eso no se muestran controles para esas operaciones. No se confunde un simulacro del calendario con una evaluación resoluble.

Procesos internos sin endpoint: `ProgramadorResumen` (correo/resumen docente preexistente), `ProgramadorAsistencia` (nuevo cierre y avisos). No se ofrece disparo sin autorización en una ruta pública. `ConflictoPersistenciaAdvice` convierte conflictos de integridad/bloqueo a 409.

## Acceso de demostración verificado

No hay contraseñas locales. Se aceptan access JWT HS256 con `user_id`, `token_type=access` y `exp`. Los roles se consultan en la base; el token no puede elevarlos. El lanzador demo usa clave aleatoria solo en memoria y limita el backend a `127.0.0.1` mediante el perfil existente. No se guarda ninguna credencial en Git ni archivos públicos.

| Rol | Usuario demo | Método | Obtención | Acceso |
|---|---|---|---|---|
| Docente | `1`, `docente@example.invalid` | JWT HS256 | Copiar token del bloque DOCENTE de `npm.cmd run demo` | Sección Demo A, consultas, banco, asistencia, reportes |
| Alumno | `2`, `alumno@example.invalid` | JWT HS256 | Copiar token del bloque ALUMNO del mismo proceso | Matrícula Demo A, prácticas, consultas propias |
| Apoderado | `3`, `apoderado@example.invalid` | JWT HS256 | Copiar token del bloque APODERADO del mismo proceso | Solo alumno 2: calendario y avisos |

Los tres tokens se verifican contra Spring real en la prueba E2E. Vencen a las dos horas en el lanzador y se invalidan al reiniciar el backend con otra clave. En el portal se pegan en «Token de acceso». Se pueden usar tres ventanas/pestañas; la sesión queda en `sessionStorage` y «Cerrar sesión» la limpia. No compartir la consola con tokens en capturas públicas.

En producción, utilizar el access token real emitido por Django y configurar `UMBRAL_JWT_SECRET` compartido, de al menos 32 bytes. No se implementa un emisor demo público, login falso ni omisión de JWT.

## Ejecución exacta en PowerShell

JDK 25 o superior; en este equipo se verificó con OpenJDK 26. Node 24. Se usa `npm.cmd` porque la política local bloquea `npm.ps1`.

Primera terminal, compilar backend (desde `C:\UMBRAL`):

```powershell
$env:JAVA_HOME='C:\Users\sheil\.jdks\openjdk-26.0.2.1'
cd C:\UMBRAL\backend
.\mvnw.cmd -o -B -Pdemo package
```

Se usó `-o` con dependencias ya disponibles porque el truststore predeterminado produjo errores PKIX. En otro equipo con certificados/dependencias configurados, omitir `-o` para la primera descarga; no desactivar TLS.

Luego, en esa terminal, iniciar backend demo con tokens locales:

```powershell
cd C:\UMBRAL\frontend
npm.cmd ci
npm.cmd run demo
```

Segunda terminal:

```powershell
cd C:\UMBRAL\frontend
npm.cmd run dev
```

Abrir `http://127.0.0.1:5173` y pegar el token del perfil. El proxy `/api` de Vite apunta a `http://127.0.0.1:8080`; backend y frontend son procesos distintos. Detener con Ctrl+C. El lanzador demo requiere el JAR generado con `-Pdemo` y `JAVA_HOME` en su terminal. H2 está en memoria: los datos persisten entre solicitudes, pero se reinician al detener demo.

Backend PostgreSQL normal: configurar `UMBRAL_DB_URL`, `UMBRAL_DB_USERNAME`, `UMBRAL_DB_PASSWORD`, `UMBRAL_JWT_SECRET`; preparar esquema administrativo compartido conforme a `backend/CONTRATO_ACADEMICO.md`; ejecutar `java -jar backend/target/umbral-backend-0.0.1-SNAPSHOT.jar` sin perfil demo. Flyway V9 crea solo tablas propias. No se cargan V3/V900/V901 demo en producción.

Frontend de producción: `npm.cmd run build` genera `frontend/dist`; servir estáticos con HTTPS y proxy `/api` al backend. `preview` permite inspección local con proxy a 8080; no configura el servidor de producción.

## Verificación reproducible

```powershell
cd C:\UMBRAL\backend
.\mvnw.cmd -o -B -Pdemo package
cd ..\frontend
npm.cmd test
npm.cmd run test:browser
npm.cmd run build
npm.cmd run test:e2e
```

E2E inicia backend H2 propio en 18089 y Vite en 5178, genera JWT efímeros, usa Edge instalado y detiene sus procesos al terminar. Puede configurarse otra ruta Chromium con `UMBRAL_BROWSER`. No utiliza el backend del usuario en 8080. Evidencia local ignorada por Git: `frontend/test-results/`. Los informes Maven están en `backend/target/surefire-reports/`.

La suite inicial pasó 52 pruebas. Se agregaron dos pruebas HTTP integrales (cuenta y HU36), para un total de 54. El cliente tiene tres pruebas sobre escape de contenido, errores HTTP y cierre por JWT vencido. La prueba de navegador cubre login de tres roles, prácticas, solucionario, dos consultas agrupadas, respuesta, resolución, filtro, historial, PDF, programación/presencia, calendario, avisos, ancho móvil y logout.

Además se conservan once escenarios de regresión del trabajo remoto: catálogo público, anchos 375/768/1440, filtros, estados vacíos, validación de espacios, resultados confirmados y limpieza, seis estados HTTP y recuperación ante fallo de red. La prueba integral ampliada verifica edición/rechazo del banco, respuesta e historial docente y un aviso de inasistencia creado automáticamente y leído desde el portal. Usa puertos 18089/5178; regresión usa 18091/5179.

No se afirma haber probado PostgreSQL real, SMTP real, todas las combinaciones de navegadores ni una integración IA inexistente.

## Archivos y explicación para la exposición

| Archivo o grupo | Responsabilidad |
|---|---|
| `frontend/src/main.js` | Vistas reales por perfil, navegación y eventos; reutiliza rutas verificadas. HU31: `consultas/groupsHtml`; HU32: formulario `data-group-response` y lectura de respuesta; HU33: `data-resolve` e historial; HU36: `clases/notificaciones`. |
| `frontend/src/api.js` | Bearer JWT, errores HTTP, fin de sesión, escape de texto y filtros codificados. |
| `frontend/src/componentes.js`, `practicas.js`, `dudas.js`, `sesion.js`, `styles.css` | Módulos remotos preservados. La navegación actual reutiliza validación y resultados; la API mantiene sus contratos exportados. |
| `frontend/src/style.css`, `frontend/index.html` | Identidad verde/crema, Bootstrap, adaptación móvil y estructura accesible. |
| `frontend/package*.json`, `vite.config.js` | Dependencias reproducibles, scripts y proxy del desarrollo. |
| `frontend/scripts-demo.mjs` | Arranque demo y JWT locales efímeros de usuarios existentes. |
| `frontend/tests/*` | Seguridad del cliente y flujo integral con Edge. |
| `CuentaController.kt` | Identidad y relaciones autorizadas para las pantallas. |
| `AsistenciaWebController.kt`, `AsistenciaWebService.kt`, `ProgramadorAsistencia.kt` | HU36: programar, registrar presentes, cerrar transaccionalmente y notificar a la familia correspondiente. |
| `V9__clases_asistencia_notificaciones.sql` | Tablas propias, índices y restricciones de unicidad. |
| `V901__enunciados_academicos.sql` | Limpieza de prefijo técnico únicamente en enunciados demo; no modifica checksums antiguos. |
| `DocenteController.kt`, `SolucionarioService.kt` | Nueva lectura autorizada de solucionarios para revisar borradores después de recargar. |
| `RespuestasHttpTests.kt` | Pruebas nuevas de identidad, cierre programado, persistencia y permisos. |
| `.gitignore` | Excluye evidencias de navegador generadas. |

Los archivos nuevos y los cambios relevantes incluyen comentarios en español. Se reutilizan sin reescribir `DudaService`, `RespuestaGrupoService`, `ResolucionDudaService` y sus repositorios: el trabajo ya implementado conserva transacciones, bloqueos y reglas.

Guion sencillo:

1. Alumno: enviar dos consultas con el mismo tema. Docente: abrir el centro; se ve un grupo con dos consultas (HU31). La agrupación actual usa reglas, **no IA**.
2. Docente: responder al grupo. Alumno: consultar ambas respuestas; cada duda apunta a la misma respuesta almacenada (HU32).
3. Alumno o docente: marcar una duda respondida como resuelta. Filtrar resueltas y abrir historial; el estado se conserva al recargar (HU33).
4. Docente: programar clase con finalización próxima y registrar solo los presentes. Al finalizar, el programador cierra y crea avisos internos para las familias de los ausentes. Apoderado: abrir Notificaciones; solo se muestran sus hijos vinculados (HU36). Si se marcó presente al único alumno demo, no habrá aviso: es el resultado correcto. El cierre manual antes de la hora devuelve conflicto y no crea avisos.

## Límites y dependencias pendientes

- HU31 semántica con IA: hace falta elegir proveedor, contrato, política de privacidad, credencial de entorno y pruebas con ese servicio. La normalización de temas no se presenta como inteligencia artificial.
- HU36 cubre clases que se programan en el portal; el contrato Django no contiene sesiones fechadas ni asistencia central. Sin esa integración no detecta clases registradas exclusivamente en otro cliente. La tarea corre cada 60 segundos con la aplicación activa (`umbral.asistencia.intervalo-ms`); recupera clases vencidas al reiniciar. Una aplicación detenida no garantiza notificación el mismo día.
- Ausente significa matrícula activa sin registro de presencia al cierre. No se modelan justificaciones, cancelaciones ni correcciones posteriores al cierre. No se envía correo/SMS/push de ausencia: solo aviso interno real.
- La consulta de resultados individuales, evolución y notificaciones generales de alumno/apoderado necesita endpoints autorizados que no existen. Se muestran las herramientas disponibles, sin inventar notas.
- Extracción de boletín y variantes existentes contienen datos fijos. Se preservan para no romper clientes, pero no se ofrecen en la web hasta reemplazarlas por una integración real y probarla.
- El solucionario existente resuelve ecuaciones lineales enteras compatibles y valida el resultado; otros enunciados devuelven 422. No es un generador general con IA.
- El correo semanal docente existente se simula deliberadamente en demo/test; SMTP real requiere configuración de entorno y validación independiente. No tiene relación con los avisos internos de HU36.
- La búsqueda de grupos filtra datos autorizados ya recuperados. Para academias grandes conviene añadir paginación/agregación al API; ahora se consulta cada grupo para obtener cantidades exactas.
- PostgreSQL: SQL portable y migración nueva comprobada en H2, pero falta ejecución contra un servidor PostgreSQL configurado. No se han alterado tablas administrativas ni enviado datos ficticios a producción.

## Git

Se revisan únicamente archivos de esta entrega antes de cada commit; no se incorpora `backend.zip`, archivos locales ajenos ni secretos. Los identificadores y el estado remoto definitivo se informan al terminar. No se cambia de rama, no se reescribe historial y el push será normal después de las verificaciones.

| Commit | Cambio verificado |
|---|---|
| `abe681f` | HU36, contexto de cuenta, pruebas HTTP de identidad y cierre programado. |
| `4fcf338` | Lectura autorizada del solucionario para revisión docente. |
| `7098b8e` | Portal por roles, consultas HU31/32/33, prácticas, reportes, demo y pruebas integrales. |
| `b356fc2` | Merge de los seis avances remotos, catálogo público y resultados de sesión preservados, regresión adaptada y hora de Lima. |

La documentación se registra en un commit adicional. El identificador final y el resultado del push se indican en la entrega de la conversación; no se introduce una referencia circular al propio commit.
