# Avance de Sheila — backend web UMBRAL

Actualizado: 27 de octubre de 2026. Rama autorizada utilizada: `web`.

## Estado real

Las ocho historias tienen implementación Kotlin/Spring Boot y commits publicados.
No se declara aceptación Scrum definitiva: los documentos UMBRAL no están en el
repositorio ni fueron proporcionados durante esta sesión. Se implementó el alcance
del mensaje de Sheila y se documentaron las políticas provisionales en
[CONTRATO_ACADEMICO.md](CONTRATO_ACADEMICO.md).

| HU | Implementación comprobada | Pendiente para cierre formal |
| --- | --- | --- |
| 42 | Preguntas aprobadas por tema/dificultad; respuestas persistidas; validación de opción, identidad, matrícula y docente activo. | Contrastar criterios Scrum y contrato de IDs con despliegue administrativo. |
| 27 | Respuesta correcta y solución APROBADO solo después de un intento propio, incluso incorrecto. Sin acceso a BORRADOR. Se protegieron rutas docentes que permitían eludir la restricción. | Validación integrada con tokens emitidos por el Django desplegado. |
| 28 | Registro de dudas; agrupación por docente, sección y pregunta, o tema normalizado; persistencia y aislamiento. | Confirmar criterio de similitud. No se implementó similitud semántica ni IA y no se afirma que existan. |
| 29 | Respuesta docente inmutable vinculada a las dudas elegibles; historial de respuestas; permisos. Dudas posteriores requieren otra respuesta. | Confirmar política Scrum de agrupación/respuesta. |
| 30 | Resolución idempotente; filtro PENDIENTE/RESUELTA; historial con actor y fecha; migración conserva eventos previos. | Confirmar política provisional: alumno propietario o docente asignado pueden resolver después de una respuesta. |
| 38 | Calendario del hijo vinculado usando tablas existentes de Lucas; matrícula/sección activas; filtros de fecha y exclusión de cancelados. | Conexión al esquema administrativo real. El modelo disponible solo registra fecha, no hora. |
| 39 | Descarga PDF por sección con resultados persistidos, promedio, detalle y temas débiles; permisos; apertura y renderizado verificados. | Confirmar presentación/indicadores del documento Scrum. |
| 23 | Estadísticas de semana completa; resumen persistido; programación configurable; reintentos; correo simulado obligatorio en demo/test; adaptador SMTP normal. | Confirmar horario/métricas Scrum. Configurar y verificar SMTP real cuando corresponda; no se enviaron correos reales. |

## Endpoints

Salvo el catálogo de preguntas, requieren `Authorization: Bearer <access token>`.
El rol y los identificadores propios se obtienen de la identidad verificada y del
esquema académico; los parámetros de ruta no conceden permisos.

| HU | Método y ruta | Actor / detalle |
| --- | --- | --- |
| 42 | `GET /api/practicas/preguntas?tema=Algebra&dificultad=FACIL` | Público, no revela respuestas. |
| 42 | `POST /api/practicas/respuestas` | ALUMNO. JSON `{"preguntaId":1,"respuesta":"4"}`. 201. |
| 27 | `GET /api/practicas/preguntas/{id}/solucionario` | ALUMNO con intento propio; anticipado 403, sin solución aprobada 404. |
| 28 | `POST /api/alumno/dudas` | ALUMNO. JSON `{"docenteId":1,"seccionId":1,"preguntaId":1,"texto":"Mi duda"}`; sin pregunta, indicar `tema`. 201. |
| 28/30 | `GET /api/alumno/dudas?estado=PENDIENTE` | Propias; estado opcional PENDIENTE/RESUELTA. |
| 28 | `GET /api/docente/dudas/grupos` | DOCENTE, solo grupos autorizados. |
| 28/30 | `GET /api/docente/dudas/grupos/{id}?estado=RESUELTA` | Dudas del grupo; estado opcional. |
| 29 | `POST /api/docente/dudas/grupos/{id}/respuestas` | DOCENTE. JSON `{"texto":"Explicación"}`. 201; sin dudas elegibles 409. |
| 29 | `GET /api/docente/dudas/grupos/{id}/respuestas` | Historial de respuestas del grupo. |
| 29 | `GET /api/alumno/dudas/{id}/respuesta` | Solo alumno propietario. |
| 30 | `PUT /api/alumno/dudas/{id}/resolver` | Propietario; sin respuesta 409. |
| 30 | `PUT /api/docente/dudas/{id}/resolver` | Docente asignado. |
| 30 | `GET /api/alumno/dudas/{id}/historial` | Propietario. |
| 30 | `GET /api/docente/dudas/{id}/historial` | Docente asignado. |
| 38 | `GET /api/apoderado/alumnos/{alumnoId}/simulacros?desde=2026-11-01&hasta=2026-11-30` | APODERADO vinculado; fechas inclusivas y opcionales. |
| 39 | `GET /api/docente/secciones/{id}/rendimiento.pdf` | DOCENTE asignado; application/pdf, attachment, no-store. |
| 23 | `GET /api/docente/resumen-semanal?semana=2026-10-19` | DOCENTE; lunes de una semana completa. Sin parámetro usa la semana anterior. |

El adaptador de identidad reutiliza por lectura las tablas Django inspeccionadas
en `origin/feature/lucas-base-django` (`5afd48c`). No se modificó el módulo administrativo de Lucas,
Darío, frontend ni mobile. Las rutas docentes preexistentes ahora requieren rol y
propiedad/asignación para impedir lectura anticipada de respuestas.

## Migraciones y perfiles

- V0, V1 y V2 existentes no fueron modificadas.
- V4: respuestas de práctica. V5: grupos y dudas. V6: respuestas agrupadas.
- V7: historial y recuperación de fechas originales. V8: resúmenes semanales.
- Migraciones aditivas, sin borrar tablas/datos ni activar clean, baseline u outOfOrder.
- PostgreSQL normal conserva datasource por variables UMBRAL_DB_* y Hibernate validate.
- H2 de ejecución solo se incorpora con Maven `-Pdemo` y Spring `demo`.
- Los fixtures `db/demo` y `db/identity-demo` solo se cargan en demo/test; Kotlin
  no administra las tablas Django en el perfil normal.
- JWT: HS256 compatible con SimpleJWT, exige access, exp y user_id. Roles y
  actividad se consultan en base. Configurar UMBRAL_JWT_SECRET por entorno;
  sin clave compartida, no se aceptan tokens externos. No hay login duplicado.
- Normal: correo desactivado por defecto; SMTP y activación exclusivamente por
  variables. Demo/test: siempre CorreoResumenSimulado, estado SIMULADO.

## Pruebas ejecutadas y evidencia

Por historia se ejecutó Maven antes del commit/push: 28, 29, 31, 32, 33, 35, 38 y
41 pruebas respectivamente, todas correctas en cada entrega publicada.

Verificación ampliada final:

- `mvnw.cmd -B test`: **43 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**, H2 en modo PostgreSQL.
- Misma suite contra **PostgreSQL 17.4 real**, en instancia temporal local y
  esquema aislado: **43 pruebas, 0 fallos, 0 errores, 0 omitidas; BUILD SUCCESS**.
- Incluye peticiones HTTP reales a Tomcat en puerto aleatorio con JWT firmado,
  repositorios reales, Flyway e Hibernate validate. No solo MockMvc.
- Verifica 401/403/400/404/409, aislamiento entre alumnos/docentes, bajas de
  usuario/matrícula/vínculo, intentos propios, agrupación y vínculos de respuestas.
- Prueba evolutiva V6 → V8 conserva pregunta, duda y respuesta preexistentes y
  recupera eventos con fechas originales. Usa esquema nuevo, sin limpiar datos.
- PDF descargado con registros persistidos de 16 y 20: promedio esperado 18.00;
  apertura, extracción de texto y renderizado correctos. También prueba multipágina.
- Resumen semanal: límites inclusivo/exclusivo, exclusión de otra semana/docente,
  correo simulado, ejecución repetida sin reenvío y reintento tras fallo de correo.
- `mvnw.cmd -B -DskipTests package`: BUILD SUCCESS. Se inspeccionó el JAR normal:
  driver PostgreSQL presente y H2 ausente. Las pruebas se habían ejecutado antes.
- `mvnw.cmd -B -Pdemo -DskipTests package`: BUILD SUCCESS; JAR con H2 comprobado.
- `scripts/Verificar-Demo.ps1`: **17 solicitudes HTTP correctas** contra el JAR
  demo en 127.0.0.1:18082. Incluye acceso anticipado 403, intento y solucionario,
  dudas/respuesta/resolución/historial, calendario y vínculo ajeno 403, resumen y PDF.
- El programador se ejecutó realmente en demo con intervalo de prueba de un
  segundo y produjo correo SIMULADO, sin conexión SMTP. El intervalo normal
  configurable conserva 15 minutos; no se modificó para esta verificación.
- El PDF del demo también se abrió con pypdf: una página, dos resultados,
  promedio 13.00. Los datos proceden del fixture demo existente.
- Los procesos de prueba del JAR demo y PostgreSQL temporal fueron detenidos;
  no se detuvo la instancia demo previa del usuario ni otros procesos Java.
- JDK utilizado: OpenJDK 26.0.2.1 para el proyecto que requiere Java 25.
- La nueva prueba evolutiva inicialmente detectó un problema del ciclo de vida de
  conexiones H2 al usar conexiones físicas independientes. Se ajustó al pool
  Hikari usado por Spring y se repitieron las suites completas con éxito.

Evidencia local (ignorada por Git):
`target/hu42-test.log`, `hu27-test.log`, `hu28-test.log`, `hu29-test.log`,
`hu30-test.log`, `hu38-test.log`, `hu39-test.log`, `hu23-test.log`,
`verificacion-final-h2.log`, `verificacion-final-postgresql.log`,
`package-normal.log`, `hu39-rendimiento.pdf` y `hu39-rendimiento.png`.
Además: `package-demo.log`, `smoke-demo-resultados.json`,
`smoke-demo-stdout.log`, `smoke-demo-stderr.log`, `smoke-demo-rendimiento.pdf`.

Para repetir el demo con JDK >=25 en JAVA_HOME (primero generar el JAR con
`mvnw.cmd -B -Pdemo package`), desde backend:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Verificar-Demo.ps1 -JavaHome $env:JAVA_HOME
```

La opción de ejecución se limita a ese proceso: Windows bloqueaba scripts por
defecto. No se cambió la política permanente. El script inicia y detiene su propia
instancia, genera una clave efímera y no guarda tokens ni credenciales. Usa un
puerto distinto al demo previo; admite `-Port` para cambiarlo.

Para repetir las pruebas PostgreSQL, usar exclusivamente una base/esquema de
pruebas aislado, nunca una base desplegada. En esta sesión se ejecutó Maven con
URL `jdbc:postgresql://127.0.0.1:55439/postgres?currentSchema=final_hu43`, driver
`org.postgresql.Driver`, usuario temporal `umbral_test`, sin contraseña en el
clúster local efímero, y propiedades Flyway schemas/default-schema `final_hu43`.
El proceso escuchaba solo en 127.0.0.1. La prueba evolutiva crea otro esquema
temporal; no ejecuta clean ni borra tablas.

## Commits publicados

Cada entrega se revisó, se probó, se publicó exclusivamente a `origin/web` y se
comparó HEAD con `git ls-remote origin refs/heads/web`. Sin force push ni push a main.

| HU | Hash publicado |
| --- | --- |
| 42 | `c35af18cdbb4620b1457e2cce4fd9100e46f89fa` |
| 27 | `9753b8ba18155ae85c0016824731f4ce82c8702e` |
| 28 | `fe66aaa6bda0b050608b39b3aea0d5e343ab755d` |
| 29 | `8ca084b5b8206c3c9973f3d3d290ce814801fbfa` |
| 30 | `7d7be66edc457c954cc87acfe72c6fa8ae258eb3` |
| 38 | `bf5fc67d0fb1765b0f0df54c48ad64cdd1d7ee72` |
| 39 | `ac309b0e5819193d777a548df49d1d7053c87f10` |
| 23 | `6f7f91b735af6cfa026f90e96130d0f9035b6dd9` |

Entrega anterior HU 42 catálogo: `e618be6f5e5f0d23b2a891ed24f493fad064d479`.

Verificación final, refuerzo de bajas académicas y ajuste del ancho del PDF:
`701025643361113f57c02c48b3719250404d9210`, publicado y comprobado con ls-remote.
El hash de publicación de este informe se obtiene con
`git log -1 -- backend/AVANCE_SHEILA.md`.

## Dependencias y funcionalidades no acreditadas

1. Entregar criterios Scrum UMBRAL. SITES10.pdf fue inspeccionado: corresponde a
   Aforo Light; no contiene estas historias. Las consultas a Sheila sobre reglas
   de agrupación, actores, horario y estadísticas quedaron sin respuesta.
2. Configurar integración desplegada: esquema Django accesible desde el datasource
   Kotlin, IDs de Perfil coherentes con datos existentes y secreto de firma
   compartido. Las pruebas usan tokens firmados y fixtures compatibles; no se
   hizo login contra un Django real ni se modificó su configuración.
3. Antes de desplegar en una base existente, revisar esquema e historial Flyway,
   especialmente V0 y convivencia con tablas Django. No se activó baseline
   automático ni se modificó una base desplegada.
4. Confirmar criterio de similitud de HU 28. Una exigencia de IA/semántica requiere
   proveedor y contrato adicionales: esa funcionalidad no está implementada.
5. SMTP real requiere configuración y validación posteriores. El cron inicial es
   lunes 08:00 America/Lima; reintentos cada 15 minutos, máximo 3, configurables.
   SMTP no garantiza entrega exactamente una vez ante una caída posterior al envío
   y anterior al commit. ENVIADO significa aceptación SMTP, no recepción en bandeja.
6. El informe PDF usa resultados del backend Kotlin existente. No se implementó
   una sincronización nueva con adm_resultado de Django.

Los archivos locales previos `.run/`, `backend.zip`, `SITES10.pdf`,
`backend/CONEXION_LOCAL.md` y `backend/DEMO_LOCAL.md` se conservaron fuera de los
commits. La configuración demo previa se revisó, conservó y publicó junto a HU 42.
La guía demo anterior describe endpoints públicos docentes anteriores al JWT;
para el comportamiento actual consultar este informe y CONTRATO_ACADEMICO.md.

## Mejoras verificadas el 29/10/2026

- La revisión inicial pasó las 43 pruebas existentes, antes de modificar archivos.
- Una prueba de regresión nueva reprodujo un fallo previo con fechas futuras
  extremas del resumen semanal: DateTimeException en vez del rechazo esperado.
  Se añadió una comprobación previa a la suma de días, conservando la regla de
  semanas completas. Los casos unitario y HTTP ahora reciben 400.
- Se añadieron motivos seguros a los errores 404 de práctica y solucionarios.
  No se alteraron los códigos, los DTO ni el formato público de las respuestas.
- Se ampliaron las pruebas de JSON inválido, referencias inexistentes y límites
  de longitud. Comprueban que los errores no dejen datos parciales y que una
  solicitud válida posterior siga funcionando. Suite: 52 pruebas correctas.
- Se verificaron compilación y empaquetado demo, más 17 comprobaciones HTTP del
  script existente, incluida descarga PDF y correo programado simulado.
- No se modificaron configuración PostgreSQL/H2, migraciones, dependencias de
  la aplicación, frontend ni mobile. Los archivos locales previos se conservaron.
- Guía actualizada de ejecución y exposición: [GUIA_DEMOSTRACION.md](GUIA_DEMOSTRACION.md).

Commits publicados y comprobados en origin/web:

| Mejora | Commit |
| --- | --- |
| Validaciones y regresión de fechas | `ca5cbdf2d3e4ba42e8cfa7f81d5ff786cafcc99a` |
| Motivos de error y pruebas de servicio | `6ed828315ec30cc6f476076165500db37722a306` |
| Pruebas HTTP de límites y persistencia | `063c480c9cbf0eecf3fe8469d67deb04c29b1731` |

Evidencia local: target/revision-29-baseline.log (43 pruebas),
revision-29-fecha-antes.log (fallo previo reproducido), revision-29-validaciones.log
(45 pruebas), revision-29-errores.log (49), revision-29-pruebas.log (52) y
revision-29-documentacion.log (52 y empaquetado demo). Todos estos archivos se
conservan en target, fuera de Git.
