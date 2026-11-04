# UMBRAL — acceso convencional y campus preuniversitario

Actualización del 04/11/2026 en la rama `web`. Este documento sustituye las instrucciones de acceso manual con tokens de la entrega anterior.

## Cambios entregados

- Portada pública orientada a preparación universitaria: navegación, tres fotografías educativas WebP locales, carrusel interactivo, herramientas académicas y pie de página.
- Carrusel Bootstrap con controles, indicadores, intervalo de seis segundos, pausa por interacción, botón de pausa/reanudación y respeto de movimiento reducido. Menú desplegable en móviles.
- Login real con correo o usuario y contraseña, visibilidad de contraseña, validación, carga, errores comprensibles y panel según la identidad que devuelve el servidor. No se muestran tokens al usuario.
- Hover moderado, foco visible, controles deshabilitados, iconos vectoriales consistentes y adaptación a pantallas pequeñas.
- Se conservan agrupación de dudas, respuesta compartida, resolución, asistencia y avisos internos, banco docente, solucionarios y reportes.
- Historial de prácticas persistido: al recargar o cerrar sesión, los intentos permanecen en la base y solo los puede consultar su alumno propietario.
- Calendario propio del alumno a partir de sus matrículas; se conserva el calendario del apoderado con validación del vínculo.
- Importación real de boletines PDF estructurados y variantes matemáticas de ecuaciones lineales. Se reemplazó el contenido fijo anterior; estas funciones tienen interfaz docente.

## Autenticación y seguridad

`POST /api/auth/login` recibe JSON con `usuario` y `password`. Consulta `auth_user` y `adm_perfil`, exige usuario activo y rol ALUMNO, DOCENTE o APODERADO, verifica la contraseña y solo entonces emite un access JWT HS256 válido durante treinta minutos. `GET /api/cuenta` confirma la identidad y las relaciones autorizadas antes de mostrar el panel.

Los JWT externos anteriores siguen aceptándose con el mismo contrato y la clave compartida `UMBRAL_JWT_SECRET`. El emisor y el verificador usan el mismo bean de clave. Sin clave de entorno se genera una efímera por proceso, compatible con el funcionamiento demo previo. No se desactiva Spring Security ni se relajan permisos académicos.

Se reutilizan contraseñas del esquema Django: `bcrypt_sha256` (BCrypt con prehash SHA-256) y `pbkdf2_sha256`. Los fixtures nuevos guardan exclusivamente hashes BCrypt de coste 12. No hay contraseñas en texto plano, credenciales de producción ni claves de firma en el repositorio. La migración V902 se ejecuta solo en `db/identity-demo` con perfiles demo/test; no modifica tablas administrativas de PostgreSQL normal.

El error de acceso es uniforme para contraseña equivocada, usuario inexistente, cuenta inactiva o rol no admitido. Se valida longitud sin registrar valores rechazados de contraseña. El login devuelve `Cache-Control: no-store`. Hay límite de veinte intentos por dirección de conexión cada cinco minutos, con mapa acotado y sin confiar en cabeceras de IP enviadas por visitantes.

El cliente administra el access token automáticamente en `sessionStorage` durante la pestaña; no lo muestra, no guarda contraseñas y limpia el acceso al cerrar sesión o recibir un 401. El token se envía como Bearer, no como cookie: no se añadió autenticación por cookies ni un cambio de CSRF que afecte a clientes móviles. Producción debe usar HTTPS, protección frente a XSS y limitación distribuida de intentos en el proxy si hay varias instancias. Cerrar sesión borra la credencial del navegador; un JWT copiado previamente sigue válido hasta su expiración. No se implementa refresh ni recuperación de contraseña sin un proveedor administrativo.

## Cuentas demo

| Rol | Usuario | Correo alternativo | Contraseña |
|---|---|---|---|
| Alumno | `alumno.demo` | `alumno@example.invalid` | Entregada en la conversación, fuera de Git |
| Docente | `docente.demo` | `docente@example.invalid` | Entregada en la conversación, fuera de Git |
| Apoderado | `apoderado.demo` | `apoderado@example.invalid` | Entregada en la conversación, fuera de Git |

Los usuarios corresponden a los perfiles 2, 1 y 3 existentes. El apoderado 3 solo tiene vinculado al alumno 2. Las tres contraseñas definitivas fueron verificadas contra un backend demo sin sustitución de contraseñas de pruebas.

Las suites de navegador utilizan otra contraseña aleatoria solo en memoria mediante `UMBRAL_DEMO_TEST_PASSWORD`: el bean `DemoAccesoConfig`, exclusivo de demo, aplica hashes efímeros a los tres fixtures. Esta variable no debe usarse al iniciar la demostración habitual. No se imprimen contraseñas ni tokens en los scripts de arranque y las trazas de navegador están desactivadas para no capturarlos.

## Funciones por perfil

| Perfil | Herramientas reales |
|---|---|
| Público | Portada, carrusel, menú móvil, catálogo con tema/dificultad y acceso convencional. |
| Alumno | Inicio personalizado, prácticas, respuestas evaluadas y persistidas, solucionario autorizado tras intento, historial con filtro de resultado, calendario de simulacros, consultas por sección/docente, respuesta recibida, historial y resolución, cuenta y logout. |
| Docente | Inicio, grupos y filtros de dudas, respuesta compartida, historial, resolución, importación PDF, banco con filtros, edición/aprobación/rechazo, variantes de ecuaciones, generación/revisión/aprobación de solucionarios, resumen semanal, temas débiles, PDF, programación de clases, asistencia y cierre, cuenta y logout. |
| Apoderado | Inicio propio, alumnos vinculados, calendario por alumno y rango, notificaciones internas de inasistencia, cuenta y logout. |
| Administrador | El módulo administrativo Django mantiene su responsabilidad; no se concede acceso docente a un administrador. |

## Historias solicitadas

| Historia | Implementación | Pruebas |
|---|---|---|
| HU31 | Grupos reales por docente/sección y tema o pregunta; recuento, búsqueda de tema, filtro de estado y preguntas relacionadas. | Normalización, HTTP, dos consultas agrupadas y filtros en navegador. |
| HU32 | Respuesta persistida una sola vez y vinculada a todas las consultas pendientes sin respuesta del lote. El alumno solo consulta su propia respuesta. | Propagación, permisos, persistencia y lectura en navegador. |
| HU33 | Estado resuelto persistido e historial, con filtros. Requiere respuesta previa; docente asignado o alumno propietario. | HTTP de idempotencia y permisos; resolución desde ambos perfiles en navegador. |
| HU36 | Cierre programado de clases del portal, exclusión de presentes y avisos internos por alumno/apoderado vinculado. | Cierre programado, unicidad, privacidad, presencia e inasistencia real visible en el portal. |

HU31 conserva agrupación determinista; no se presenta como IA semántica. HU36 no envía correos/SMS/push ni detecta clases registradas únicamente en otros clientes sin integración adicional.

## Inventario completo de endpoints Kotlin

Todas las rutas siguientes llevan prefijo `/api`. Se indica la pantalla, datos requeridos y resultado correcto. HTTP significa pruebas Spring con servidor y H2; E2E significa recorrido real con Edge. Las restricciones de propiedad y vínculo se mantienen en el servidor, aunque un visitante escriba una ruta manualmente.

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
| GET `/docente/secciones/{id}/rendimiento.pdf` | Docente de la sección | 200 PDF descargable sin caché | Resultados; PDFBox, HTTP y E2E. |
| GET `/docente/resumen-semanal` | Docente propio; lunes `semana` opcional | 200 estadísticas propias de semana completa | Resultados; HTTP y E2E. |
| GET `/docente/clases` | Docente propio | 200 clases | Clases; HTTP y E2E. |
| POST `/docente/clases` | Docente asignado; `seccionId,titulo,fin` | 201 clase futura | Clases; validación HTTP y E2E. |
| GET `/docente/clases/{id}/alumnos` | Docente propietario | 200 matrícula y presencia | Clases; permisos HTTP y E2E. |
| PUT `/docente/clases/{id}/presentes/{alumnoId}` | Docente; alumno matriculado, clase abierta | 204 | Asistencia; HTTP y E2E. |
| PUT `/docente/clases/{id}/cerrar` | Docente; clase finalizada | 204; 409 antes de finalización | Clases; idempotencia HTTP y mensaje de error E2E. |
| GET `/apoderado/alumnos/{alumnoId}/simulacros` | Apoderado vinculado; `desde,hasta` opcionales | 200 calendario autorizado | Calendario; privacidad HTTP y E2E. |
| GET `/apoderado/alumnos/{alumnoId}/notificaciones` | Apoderado vinculado | 200 avisos reales con alumno, descripción y fecha | Notificaciones; HTTP y proceso completo E2E. |

No se eliminaron rutas. No hay endpoints exclusivamente móviles en estos controladores; los clientes móviles pueden seguir usando los existentes. Los procesos de resumen semanal y cierre de asistencia son internos y no se exponen mediante controles públicos. La administración de matrículas, usuarios y cursos corresponde a Django.

## Importación y variantes

El PDF debe tener texto seleccionable y bloques con `Tema:`, `Dificultad:`, `Pregunta:`, opciones `A)` a `H)` y `Correcta:` con la letra. Se valida todo antes de guardar; tamaño máximo 1 MB, hasta 30 páginas y cien preguntas. Los datos importados quedan PENDIENTE para revisión docente. El formulario muestra instrucciones académicas del formato.

Las variantes transforman una ecuación lineal válida `ax ± b = c` con respuesta entera comprobada y generan coeficiente, resultado y opciones nuevos; no son una llamada a IA. Los enunciados no compatibles devuelven 422 y no guardan ejercicios de reemplazo.

## Ejecución en este equipo

```powershell
$env:JAVA_HOME='C:\Users\sheil\.jdks\openjdk-26.0.2.1'
cd C:\UMBRAL\backend
.\mvnw.cmd -o -B -Pdemo package
cd ..\frontend
npm.cmd ci
npm.cmd run demo
```

Segunda terminal:

```powershell
cd C:\UMBRAL\frontend
npm.cmd run dev
```

Abrir `http://localhost:5173/`. El backend habitual usa 8080. No se necesita copiar tokens ni configurar contraseñas en JavaScript.

Si ya hay otro backend en 8080, se puede mantenerlo y usar una instancia nueva:

```powershell
# Terminal del backend demo:
$env:UMBRAL_DEMO_PORT='8081'
npm.cmd run demo
# Terminal del frontend:
$env:UMBRAL_API_TARGET='http://127.0.0.1:8081'
npm.cmd run dev
```

H2 es efímera al detener la instancia. PostgreSQL utiliza las variables de entorno y el esquema Django ya documentados; V902 no se descubre en producción. La primera descarga Maven en otro equipo requiere conexión y certificados válidos: omitir `-o` cuando no haya dependencias locales. No se desactiva TLS.

## Pruebas reproducibles y evidencia

```powershell
cd C:\UMBRAL\backend
.\mvnw.cmd -o -B -Pdemo package
cd ..\frontend
npm.cmd ci
npm.cmd test
npm.cmd run build
npm.cmd run test:browser
npm.cmd run test:e2e
```

Ejecutar las suites de navegador secuencialmente para evitar competencia de recursos entre JVM. Requieren JDK >=25, Node 24 y Edge instalado; `UMBRAL_BROWSER` permite elegir otro ejecutable Chromium. Utilizan puertos aislados 18091/5179 y 18089/5178; no reutilizan servidores ni datos del usuario.

Las pruebas incluyen credenciales incorrectas, formatos de hash, inactividad, roles, límite de intentos, JWT aceptado, persistencia, privacidad, catálogo y estados vacíos, filtros, formularios, carga, seis errores HTTP, fallo de red, navegación, carrusel manual/automático, movimiento reducido, hover y pantallas 375/768/1440. El recorrido integral incluye los tres perfiles, preguntas, solucionarios, PDF importado, variantes, resultados persistidos, calendario del alumno, HU31/32/33, resumen/PDF docente, asistencia y aviso HU36 visible para el apoderado.

Informes locales: `backend/target/surefire-reports`, `backend/target/verificacion-login.log` y `frontend/test-results` (ignorados por Git). No se afirma haber validado PostgreSQL real, Safari/Firefox, correo externo ni IA semántica.

Resultado final verificado: **62 pruebas del backend, 3 pruebas del cliente y 18 escenarios de navegador aprobados**, además del recorrido integral de los tres perfiles. Compilaciones Maven/Vite correctas; auditoría de dependencias de producción sin vulnerabilidades reportadas. Se revisaron capturas de portada, acceso móvil y panel de alumno. La comprobación final en `http://localhost:5173` no registró errores de consola; se añadió un favicon vectorial para eliminar la solicitud 404 del navegador.

En este equipo se dejó una instancia demo nueva en 8081 y el frontend en 5173 con proxy a esa instancia. El proceso anterior de 8080 no se detuvo. Las capturas de evidencia local son `portada-final.png`, `login-movil-final.png` y `panel-alumno-final.png` dentro de `frontend/test-results`.

## Archivos y responsabilidades

- `AutenticacionController.kt`, `AutenticacionService.kt`, `PasswordAcademico.kt`, `SeguridadConfig.kt`: acceso, hashes, validación, límites y firma/verificación compartida.
- `V902__accesos_demo_bcrypt.sql`, `DemoAccesoConfig.kt`: hashes exclusivamente demo/test y credenciales efímeras de pruebas.
- `RespuestaPracticaController.kt`, `RespuestaPracticaService.kt`, `RespuestaPracticaRepository.kt`: historial propio persistido.
- `CalendarioController.kt`, `CalendarioService.kt`: consulta propia del alumno sin alterar el acceso del apoderado.
- `ImportadorBoletin.kt`, `BancoPreguntasService.kt`: PDF real, validación y variantes matemáticas.
- `ConflictoPersistenciaAdvice.kt`: errores de negocio comprensibles sin trazas ni secretos.
- `frontend/src/landing.js`, `icons.js`, `style.css`: portada, carrusel, recursos vectoriales, interacción, foco y responsive.
- `frontend/src/main.js`, `api.js`, `sesion.js`: login, navegación, paneles, multipart, errores y gestión automática de acceso.
- `frontend/public/images/*.webp`: fotografías educativas locales optimizadas; origen documentado en `FUENTES.md`.
- `frontend/scripts-demo.mjs`, `vite.config.js`: arranque sin tokens visibles y proxy configurable.
- `AutenticacionHttpTests.kt`, `BoletinVariantesTests.kt`, `RespuestasHttpTests.kt`, `frontend/tests/*`: verificación de cambios y regresión.

Los cambios incluyen comentarios útiles en español. JSON y dependencias generadas conservan su formato estándar sin comentarios. Los archivos locales previos `application.properties`, `package-lock.json`, `.run`, PDF, ZIP y notas ajenas se preservan fuera de los commits nuevos.

## Límites reales

- Agrupación por tema/pregunta, sin similitud semántica IA. Requiere elegir proveedor/contrato/credenciales para añadirla.
- Importación sin OCR y con formato explícito; variantes y solucionarios limitados a ecuaciones lineales verificadas.
- No existe evaluación resoluble de simulacros: el calendario muestra fechas reales; no se inventaron preguntas, notas ni un botón de evaluación inexistente.
- Avisos de inasistencia internos y solo para clases del portal. La aplicación activa procesa el cierre cada minuto; si está detenida, el aviso puede retrasarse.
- No hay resultados de simulacro autorizados para apoderados, recuperación de contraseña, refresh o cambio de datos personales implementados por este backend. No se muestran controles falsos.
- Los algoritmos Django Argon2/scrypt requieren soporte adicional; no se aceptan con un fallback inseguro. PostgreSQL requiere el esquema administrativo compatible y prueba contra una instancia configurada.
- No se modificaron tablas de producción, no se enviaron datos demo a PostgreSQL y no se reescribió historial Git. Los commits y la sincronización final se detallan en la entrega de la conversación.

## Commits de implementación

| Commit | Cambio verificado |
|---|---|
| `33e71ca` | Login seguro, formatos de hash, JWT compatible, fixtures BCrypt y pruebas HTTP. |
| `4de3031` | Importación de PDF real y variantes matemáticas verificadas. |
| `6a18620` | Historial persistido y calendario propio del alumno con pruebas de privacidad. |
| `6720d11` | Portada, carrusel, acceso convencional, hover, paneles, recursos locales y pruebas de navegador. |

La documentación se registra en un commit adicional. El identificador definitivo y el estado del push se informan en la conversación, sin incluir contraseñas en archivos de Git.
