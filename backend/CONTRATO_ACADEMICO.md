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

H2 se activa únicamente con los perfiles Maven `demo` y Spring `demo`. Las
tablas académicas ficticias de `db/identity-demo` solo se ejecutan en demo/test;
no son migraciones del esquema Django real. Las direcciones `example.invalid`
son datos de prueba. Para ejecutar pruebas: `mvnw.cmd -B test` con JDK >=25.

Pendiente contrastar criterios Scrum: `SITES10.pdf` corresponde a Aforo Light,
no a UMBRAL. Las reglas de este contrato implementan el alcance del mensaje
de Sheila del 27/10/2026 y deben validarse con el equipo antes del despliegue.
