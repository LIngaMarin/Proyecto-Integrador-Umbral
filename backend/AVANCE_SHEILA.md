# Avance de Sheila — backend web UMBRAL

Fecha de trabajo: 25 de octubre de 2026.

## Rama y revisión inicial

- Repositorio: https://github.com/LIngaMarin/Proyecto-Integrador-Umbral.git.
- Rama identificada mediante Git: `web`, con seguimiento de `origin/web` y commits anteriores de `SAguirreT`.
- Referencias actualizadas con `git -c http.sslBackend=schannel fetch origin --prune`.
- Revisados los últimos commits de `web` (`4767dbb`, `1448170`, `2ca3222`), `main` y `feature/lucas-base-django` (`8e033f7`, `aa3e0f9`, `3089649`), `develop` (`8742a59`, `8d6ad6f`) y `mobiles` (`8492d30`, `edc4bf6`, `41ccf04`).
- Revisados controlador, servicios, entidades, repositorios, DTO, migraciones y todas las pruebas del backend. El README describe Django, pero el backend de esta rama es Kotlin/Spring Boot.
- No se encontraron documentos del proyecto integrador ni laboratorio Scrum. Su entrega y la verificación de asignaciones y criterios formales quedan pendientes. Se usaron los requisitos del mensaje de Sheila como alcance provisional.
- `backend.zip` ya estaba sin seguimiento al comenzar; se inspeccionó su listado, sin extraerlo ni añadirlo a commits.

## Implementado y publicado

HU 42 **parcial**, primera entrega: consulta de preguntas por tema y dificultad.

- `GET /api/practicas/preguntas?tema=Algebra&dificultad=FACIL`.
- Tema y dificultad obligatorios; dificultades: `FACIL`, `INTERMEDIO`, `DIFICIL`. Tema vacío o dificultad inválida producen HTTP 400.
- Consulta únicamente preguntas `APROBADA`; sin coincidencias devuelve una lista vacía.
- DTO de salida con id, enunciado, tema, dificultad y opciones. No devuelve respuesta correcta, datos del docente ni revisión.
- Reutiliza `BancoPreguntasService` y `PreguntaRepository`; conserva los filtros docentes existentes de HU 22.
- La consulta actual es un catálogo público sin identidad de alumno. No implementa selección por matrícula/sección; esta política debe confirmarse con los documentos y el contrato de identidad.
- Se añadió V0 para crear `preguntas` y `pregunta_opciones` antes de V1 en bases nuevas y un índice para la consulta. V1 y V2 no fueron modificadas.
- La prueba de validación del esquema detectó que `pasoAPaso` no coincidía con `paso_a_paso` de V1; se corrigió el nombre explícito de la columna en la entidad existente.
- H2 solo se usa como dependencia de pruebas, con perfil `test` y validación del esquema por Hibernate. Las migraciones se ejecutan mediante Flyway.

Commit publicado y comprobado con `ls-remote`:

`e618be6f5e5f0d23b2a891ed24f493fad064d479` — `feat(hu42): implementar preguntas por tema`.

## Pruebas y compilación

- JDK usado: `C:\Users\sheil\.jdks\openjdk-26.0.2.1` (el Java predeterminado del terminal era Java 8; el proyecto requiere Java 25).
- `mvnw.cmd -Dtest=PracticaHu42Tests test`: 4 pruebas, 0 fallos, 0 errores antes de incorporar persistencia.
- Suite final: `mvnw.cmd -B test`: **25 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**. Incluye compilación de código principal y pruebas.
- Cobertura nueva: filtros y validación del servicio; HTTP y serialización con MockMvc; consulta JPA real que excluye preguntas pendientes, rechazadas, de otro tema y de otra dificultad; migraciones y arranque del contexto.
- H2 está en modo PostgreSQL; no se ejecutaron pruebas contra un servidor PostgreSQL real.
- Compilación de los archivos Kotlin modificados mediante IntelliJ: correcta. La compilación global del IDE falló por ausencia del SDK Android; no se modificó ese módulo.
- Se resolvieron errores TLS locales usando el almacén de certificados Windows para Git y Maven, sin desactivar la verificación de certificados. Para Maven se usó `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE` en el proceso.
- Durante la validación hubo fallos por falta de datasource, mapeo de columna y concurrencia entre compilaciones del IDE/Maven. Se corrigieron el entorno de pruebas y el mapeo y se ejecutó nuevamente la suite hasta obtener el resultado anterior.
- Revisados `git diff`, `git diff --cached --check` y `git status`. Solo se publicaron archivos específicos del backend. Sin cambios en frontend, Android ni otras ramas.

## Bloqueos y pendientes por historia

| Historia | Estado | Dependencia necesaria |
| --- | --- | --- |
| HU 42, registrar respuestas | Pendiente | Identidad autenticada del alumno, existencia/estado del alumno y autorización para acceder a la pregunta. No existen entidades, repositorios ni cliente de identidad/matrícula en este backend. |
| HU 27 | Pendiente | HU 42 de respuestas y permisos. `SolucionarioService` y estados BORRADOR/APROBADO ya existen; falta acreditar el intento previo del alumno para impedir acceso anticipado. |
| HU 28, modelo | Pendiente | Contrato de relaciones alumno/docente y reglas de asignación del docente, además de los documentos Scrum. No existe una entidad de dudas para reutilizar. |
| HU 28, agrupación IA | Bloqueada | No existe una integración IA real en el backend inspeccionado: `generarVariante` usa texto programado y `SolucionarioService` usa una regla de ecuaciones lineales. Faltan proveedor/cliente, contrato y manejo esperado de sus errores. |
| HU 29 | Pendiente | Modelo y agrupación HU 28; identidad y permisos del docente sobre el grupo. |
| HU 30 | Pendiente | Modelo de dudas, actores autorizados y reglas de resolución/transición que deben contrastarse con los documentos. |

No se crearon endpoints que acepten un `alumnoId` o `docenteId` proporcionado por el cliente como prueba de autorización, ni se presentó un agrupamiento programado como integración IA. No se crearon los commits de estas funcionalidades pendientes.

Antes de aplicar V0 a una base existente, revisar su esquema y `flyway_schema_history`: la migración fue verificada para una base nueva. No se conectó ni modificó ninguna base desplegada; no se activó `outOfOrder` automáticamente.

## Siguiente paso para mañana

1. Recibir y contrastar los documentos del proyecto y laboratorio Scrum, especialmente actores, asignaciones y criterios de aceptación.
2. Identificar con el equipo el contrato de autenticación, roles, alumnos/docentes y matrícula, y el cliente IA real. La consulta a Sheila sobre ese contrato quedó pendiente de respuesta.
3. Implementar y publicar el registro de respuestas de HU 42 con validaciones reales; continuar con HU 27, HU 28, HU 29 y HU 30 en el orden solicitado, con pruebas, commits específicos y verificación de cada push.
4. Validar las migraciones contra PostgreSQL y contrastar el esquema de cualquier instalación existente antes de desplegar.

Este informe se publica en un commit documental separado; su hash se obtiene con `git log -1 -- backend/AVANCE_SHEILA.md` y se comunica al verificar ese push.
