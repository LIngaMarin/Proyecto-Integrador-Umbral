# Demostración del backend web

Revisión del 29/10/2026. Esta guía usa el contrato actual de las HU de Sheila.
El detalle de permisos y reglas provisionales está en
[CONTRATO_ACADEMICO.md](CONTRATO_ACADEMICO.md); los endpoints completos y las
dependencias de despliegue están en [AVANCE_SHEILA.md](AVANCE_SHEILA.md).

## Compilar y probar

Desde `backend`, configura `JAVA_HOME` con un JDK 25 o superior y ejecuta:

```powershell
& "$env:JAVA_HOME/bin/java.exe" -version
.\mvnw.cmd -B test
```

Si este equipo presenta errores de certificados al descargar dependencias, el
ajuste de proceso utilizado en esta sesión fue:

```powershell
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
```

Las pruebas usan datos ficticios y H2 en modo PostgreSQL; incluyen HTTP real,
persistencia y migraciones. No se deben apuntar a una base desplegada.

## Demo automática sin credenciales guardadas

```powershell
.\mvnw.cmd -B -Pdemo package
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Verificar-Demo.ps1 -JavaHome $env:JAVA_HOME
```

El script inicia su propio proceso en `127.0.0.1:18082`, genera una clave y tokens
efímeros en memoria, comprueba 17 solicitudes HTTP y detiene esa instancia al
terminar. No guarda tokens ni cambia la política permanente de PowerShell.
El puerto se puede cambiar con `-Port 18083` si está ocupado.

La evidencia local queda en `target/smoke-demo-resultados.json`,
`target/smoke-demo-rendimiento.pdf` y `target/smoke-demo-stdout.log`.
El archivo PDF puede abrirse con cualquier lector compatible. `target` está
ignorado por Git. El correo de demo es siempre simulado.

## Ejecución manual

```powershell
& "$env:JAVA_HOME/bin/java.exe" -jar .\target\umbral-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=18082
```

Este comando requiere el JAR generado con `-Pdemo`; activar solo el perfil Spring
no agrega H2 al JAR normal. La base demo está en memoria: los cambios desaparecen
al detener el proceso y un nuevo arranque recupera los fixtures.

El catálogo público puede consultarse directamente:

```powershell
Invoke-RestMethod 'http://127.0.0.1:18082/api/practicas/preguntas?tema=Algebra&dificultad=FACIL'
```

Las demás rutas exigen un token de acceso firmado. Kotlin no tiene un endpoint de
login: en la integración normal los tokens se obtienen del sistema administrativo.
Para una demo manual, la instancia y los tokens locales deben compartir la clave
efímera configurada por entorno en `UMBRAL_JWT_SECRET`. No publicar esa clave ni
exportar tokens en colecciones o archivos de entorno compartidos.

## Orden de las funcionalidades

| HU | Solicitud principal | Comprobación |
| --- | --- | --- |
| 42 | `GET /api/practicas/preguntas?tema=Algebra&dificultad=FACIL` | Solo preguntas aprobadas; no devuelve la respuesta correcta. |
| 42 | `POST /api/practicas/respuestas` | Alumno. Cuerpo `{"preguntaId":1,"respuesta":"4"}`; devuelve 201. |
| 27 | `GET /api/practicas/preguntas/1/solucionario` | Requiere un intento propio y una solución aprobada. |
| 28 | `POST /api/alumno/dudas` | Cuerpo `{"docenteId":1,"seccionId":1,"preguntaId":1,"texto":"No entiendo el despeje"}`; devuelve 201. |
| 28 | `GET /api/docente/dudas/grupos` | El docente consulta sus agrupaciones autorizadas. |
| 29 | `POST /api/docente/dudas/grupos/{grupoId}/respuestas` | Cuerpo `{"texto":"Restar seis y dividir entre dos"}`; devuelve 201. |
| 29 | `GET /api/alumno/dudas/{dudaId}/respuesta` | El alumno consulta la respuesta de su propia duda. |
| 30 | `PUT /api/alumno/dudas/{dudaId}/resolver` | Requiere una respuesta; repetir la resolución no duplica eventos. |
| 30 | `GET /api/alumno/dudas/{dudaId}/historial` | Muestra REGISTRADA, RESPONDIDA y RESUELTA. |
| 38 | `GET /api/apoderado/alumnos/2/simulacros?desde=2026-11-01&hasta=2026-11-03` | Solo simulacros del hijo vinculado y de matrículas activas. |
| 39 | `GET /api/docente/secciones/1/rendimiento.pdf` | Descarga PDF; requiere docente asignado. |
| 23 | `GET /api/docente/resumen-semanal` | Calcula la semana anterior completa para el docente autenticado. |

Los IDs 1 y 2 de los ejemplos corresponden a los fixtures de una demo recién
iniciada. Para otros datos, usa los identificadores devueltos por la API.

La demo comienza sin solucionarios. El script usa los endpoints docentes ya
existentes `POST /api/docente/preguntas/{id}/solucionario` y
`PUT /api/docente/solucionarios/{id}/aprobar` para preparar la consulta de HU 27.
No se agregaron rutas nuevas para la demostración.

## Errores que se pueden explicar en la exposición

- HTTP 400: JSON inválido, opción fuera del catálogo, campos vacíos, texto que
  supera su límite o semana inválida. El resumen también rechaza fechas futuras
  extremas sin desbordar el calendario.
- HTTP 401: falta un token válido, está vencido o es de tipo refresh.
- HTTP 403: rol incorrecto, falta de relación académica o consulta anticipada.
- HTTP 404: referencia no disponible, pregunta no publicada o falta de solución
  aprobada/respuesta, según la operación.
- HTTP 409: una duda no tiene respuesta para resolverse o un grupo ya no tiene
  dudas sin responder.

Los motivos internos de práctica y solucionarios usan el mismo mensaje para una
pregunta inexistente y una no publicada. No se cambió el formato del JSON público
ni se activó la exposición general de mensajes, trazas o detalles de SQL.

## Límites que siguen pendientes

No se modificaron PostgreSQL, los perfiles, las migraciones ni dependencias de la
aplicación. La validación con el Django desplegado y SMTP real sigue requiriendo
su configuración. Los criterios Scrum de similitud, resolución y programación
deben confirmarse con los documentos del equipo; esta revisión no cambia esas reglas.
