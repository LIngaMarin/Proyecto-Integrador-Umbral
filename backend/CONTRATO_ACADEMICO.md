# Integración del backend web con administración

El backend Kotlin lee el contrato de `origin/feature/lucas-base-django`, commit
`5afd48c`: `auth_user`, `adm_perfil`, `adm_matricula`, `adm_seccion`, `adm_horario`,
`adm_apoderado`, `adm_apoderado_hijos` y `adm_simulacro`. No crea ni modifica esas
tablas en el perfil PostgreSQL. Se necesita acceso de lectura al esquema Django
en el datasource configurado. Si no está disponible, las operaciones protegidas
devuelven 503; no conceden permisos por defecto.

`Authorization: Bearer <access token>` de Django SimpleJWT, HS256, con `user_id`,
`token_type=access` y `exp`. Configurar `UMBRAL_JWT_SECRET` en el entorno con la
clave de firma de Django (al menos 32 bytes); nunca en Git. Sin esta configuración
se genera una clave efímera y los tokens de Django no serán aceptados. No hay
endpoint de login ni emisión de tokens en Kotlin. Los roles se leen de la base,
no del cuerpo ni de claims controlados por el cliente. `Actor.id` es `Perfil.id`,
no `auth_user.id`. Los identificadores docentes existentes deben referir perfiles.

Un alumno puede responder preguntas aprobadas de docentes con horario activo
en una sección activa en que tiene matrícula activa. El catálogo sigue público
y no contiene respuestas. Las respuestas admiten intentos sucesivos y conservan
fecha, opción y evaluación de cada intento.

HU 27: `GET /api/practicas/preguntas/{id}/solucionario` exige rol ALUMNO,
matrícula vigente y al menos una respuesta propia persistida. Retorna únicamente
soluciones APROBADO de preguntas APROBADA; BORRADOR devuelve 404 y el acceso
anticipado 403. El intento puede ser incorrecto. Las rutas docentes originales
también exigen rol DOCENTE y propiedad de la pregunta o acceso a la sección,
para impedir que un alumno lea respuestas a través del banco docente.

HU 28 (regla provisional pendiente de criterios Scrum): agrupación por docente,
sección y pregunta; sin pregunta, por docente, sección y tema normalizado
(minúsculas, espacios y tildes). No es similitud semántica ni IA. Las dudas de
distintas secciones no se mezclan. El alumno solo consulta sus propias dudas;
el docente solo sus agrupaciones de secciones activas asignadas. Una carrera al
crear por primera vez un grupo produce 409 reintentable y no pierde datos.

HU 29: respuesta docente inmutable por lote de dudas pendientes sin respuesta.
Se guarda el vínculo `dudas.respuesta_id` dentro de la misma transacción. Las
dudas posteriores necesitan una nueva respuesta; no heredan la anterior.
Sin dudas elegibles devuelve 409. El alumno consulta la respuesta de su propia
duda; el docente consulta el historial de respuestas de su agrupación.

HU 30 (política provisional): el alumno propietario o el docente asignado pueden
resolver una duda que ya tenga respuesta. Una segunda resolución es idempotente.
Se filtra por PENDIENTE/RESUELTA; otros valores producen 400. El historial guarda
REGISTRADA, RESPONDIDA y RESUELTA con actor y fecha; V7 recupera fechas de dudas
y respuestas existentes. No se borran dudas ni se sobrescriben eventos.

HU 38: el apoderado consulta simulacros de cursos de las matrículas activas del
hijo vinculado, usando `adm_simulacro` de Lucas. Se excluyen simulacros cancelados
y secciones inactivas, se eliminan duplicados y se ordena por fecha/id. `desde`
e `hasta` son inclusivos; sin desde se usa hoy en Lima y sin hasta no hay límite
superior. El modelo administrativo solo tiene fecha, por lo que no se inventa
una hora. No se crean nuevas tablas ni se duplican calendarios.

HU 39: PDF descargable de una sección del docente, usando resultados persistidos
en `resultados_simulacro` y reutilizando `SimulacroService` para temas débiles.
Incluye número de resultados, alumnos distintos, promedio por resultado y detalle.
Sin datos produce un PDF válido que lo indica. Solo cifras almacenadas, sin
generación aleatoria. PDFBox verifica apertura, extracción de texto y renderizado
en las pruebas. La fuente estándar conserva español; caracteres no representables
se muestran como `?`. La descarga tiene `Cache-Control: no-store`.

HU 23 (horario y métricas pendientes de validación Scrum): semana anterior
completa, lunes 00:00 inclusive a lunes siguiente exclusivo, zona America/Lima.
Se cuentan preguntas creadas, su estado aprobado actual, intentos/aciertos y
alumnos de práctica, dudas registradas, respuestas agrupadas y resoluciones.
No se usa `resultados_simulacro` para métricas semanales porque no tiene fecha.
El endpoint devuelve estadísticas propias del docente, sin parámetro docenteId.

La tarea prepara un resumen persistido por docente/semana; cron inicial lunes
08:00 de Lima. Variables `UMBRAL_RESUMEN_CRON`, `UMBRAL_RESUMEN_ZONA`,
`UMBRAL_RESUMEN_REINTENTO_MS` (900000), `UMBRAL_RESUMEN_MAX_INTENTOS` (3).
Los reintentos recuperan la última semana si el servicio estaba apagado el lunes.
La restricción única y el bloqueo de fila evitan duplicar tareas simultáneas.
No se reenvían registros ENVIADO/SIMULADO; los fallos quedan en ERROR con contador.
SMTP no garantiza entrega exactamente una vez ante una caída después de enviar
y antes de confirmar la transacción. `ENVIADO` significa aceptado por SMTP,
no confirmación de recepción en la bandeja del destinatario.

Normal: envío desactivado por defecto. Activar `UMBRAL_RESUMEN_ENABLED=true` y
configurar `UMBRAL_SMTP_HOST`, `UMBRAL_SMTP_FROM`, opcionalmente PORT (587),
USERNAME, PASSWORD y STARTTLS (true). Usar la clave compartida JWT por entorno.
Demo/test: el bean de correo es siempre simulado, incluso si hay variables SMTP;
guarda estado SIMULADO. No abre conexiones SMTP ni manda correos reales.

H2 se activa únicamente con los perfiles Maven `demo` y Spring `demo`. Las
tablas académicas ficticias de `db/identity-demo` solo se ejecutan en demo/test;
no son migraciones del esquema Django real. Las direcciones `example.invalid`
son datos de prueba. Para ejecutar pruebas: `mvnw.cmd -B test` con JDK >=25.

Pendiente contrastar criterios Scrum: `SITES10.pdf` corresponde a Aforo Light,
no a UMBRAL. Las reglas de este contrato implementan el alcance del mensaje
de Sheila del 27/10/2026 y deben validarse con el equipo antes del despliegue.
