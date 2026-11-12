# Colección Postman de las HU de Sheila

Importa `UMBRAL_Sheila.postman_collection.json` y el entorno
`UMBRAL_Demo.postman_environment.json`. La colección contiene 30 solicitudes
organizadas en ocho carpetas: HU 42, 27, 28, 29, 30, 38, 39 y 23.

Las solicitudes usan rutas existentes. Los ejemplos de respuesta se capturaron
contra una demo H2 nueva; no representan datos del despliegue. El ejemplo PDF
omite el cuerpo binario: utiliza **Send and Download** para guardar el archivo.

## Preparar una demo manual

Necesitas JDK >=25 en `JAVA_HOME` y Python 3 disponible como `py` para el ejemplo
opcional de tokens. Desde `backend`, compila el JAR demo:

```powershell
.\mvnw.cmd -B -Pdemo package
```

En la terminal que iniciará Java, genera una clave temporal y tokens para los
usuarios ficticios. Este ejemplo solo usa la biblioteca estándar de Python:

```powershell
$bytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$env:UMBRAL_JWT_SECRET = [Convert]::ToBase64String($bytes)
@'
import os, json, base64, hmac, hashlib, time
def encode(value):
    return base64.urlsafe_b64encode(value).rstrip(b'=').decode('ascii')
for name, user in [('docenteToken', '1'), ('alumnoToken', '2'), ('apoderadoToken', '3')]:
    head = encode(json.dumps({'alg': 'HS256', 'typ': 'JWT'}).encode())
    body = encode(json.dumps({'user_id': user, 'token_type': 'access', 'exp': int(time.time()) + 3600}).encode())
    signature = encode(hmac.new(os.environ['UMBRAL_JWT_SECRET'].encode(), (head + '.' + body).encode(), hashlib.sha256).digest())
    print(name + ': ' + head + '.' + body + '.' + signature)
'@ | py -
& "$env:JAVA_HOME/bin/java.exe" -jar .\target\umbral-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=18082
```

Copia los tres tokens a los valores locales/privados del entorno Postman. No
guardes la salida de la terminal, claves ni tokens en Git o en exportaciones
compartidas. Los archivos publicados tienen esos valores vacíos.

| Variable de entorno Postman | Valor |
| --- | --- |
| `baseUrl` | `http://127.0.0.1:18082` |
| `alumnoToken` | Token privado del alumno ficticio 2. |
| `docenteToken` | Token privado del docente ficticio 1. |
| `apoderadoToken` | Token privado del apoderado ficticio 3. |

La clave se configura por entorno y no cambia el perfil ni archivos de
configuración. Al cambiarla y reiniciar el demo, genera nuevamente los tokens.
Para usar el sistema administrativo real, consulta
[CONTRATO_ACADEMICO.md](../CONTRATO_ACADEMICO.md): Kotlin no ofrece login propio.

## Ejecutar las solicitudes

Selecciona el entorno y ejecuta la colección completa en su orden original:

1. HU 42 consulta el catálogo y guarda una respuesta. `preguntaId` se obtiene de
   la respuesta real; la opción `4` corresponde al fixture de esta demo.
2. HU 27 prueba el acceso anticipado a la pregunta 2 y prepara/aprueba el
   solucionario de la pregunta respondida, usando rutas docentes ya existentes.
3. HU 28 registra una duda y guarda `dudaId` y `grupoId` en variables de colección.
4. HU 29 responde el grupo y comprueba que no se acepte una respuesta duplicada
   si no hay dudas nuevas.
5. HU 30 resuelve la duda y comprueba que repetir la resolución no duplique eventos.
6. HU 38 consulta simulacros del hijo vinculado y prueba vínculo y rango inválidos.
7. HU 39 descarga el PDF y comprueba tipo, firma y caché, más el rechazo al alumno.
8. HU 23 consulta la semana anterior y rechaza una fecha de inicio en martes.

Cada solicitud tiene comprobaciones de código HTTP; los casos principales también
validan contenido y permisos. La colección crea datos ficticios. Para repetirla
desde cero, detén **tu** proceso demo con Ctrl+C e inícialo de nuevo: H2 es efímero.
Repetir solo la generación del solucionario sin reiniciar devuelve 409 porque ya
existe, de acuerdo con el contrato actual.

No ejecutes el flujo de fixtures contra datos desplegados. Para otro conjunto de
datos debes adaptar IDs, tema y opciones a registros autorizados. No compartas
exportaciones que contengan los valores privados introducidos en Postman.

## Verificaciones y límites

La colección se ejecutó mediante Newman contra el JAR demo: 30 solicitudes y
40 comprobaciones correctas, sin fallos. Los ejemplos guardan
solo cuerpos de respuestas y cabeceras de contenido/caché; las solicitudes
conservan referencias como `{{alumnoToken}}`, nunca tokens reales.

Se detectó un comportamiento previo que esta mejora conserva: el catálogo público
con `dificultad=INVALIDA` sin token responde 401 al despachar el error. Con token
válido devuelve 400. La solicitud de validación de la colección lleva un token.
Corregir ese despacho requiere revisar la seguridad de errores compartida y queda
pendiente; no se cambió la configuración para modificar códigos HTTP.

El envío programado no tiene un endpoint adicional: se comprueba en el script
demo existente y en las pruebas Kotlin con correo simulado. La colección consulta
sus estadísticas por el endpoint real de HU 23.

Para ejecutar automáticamente la colección fuera de la interfaz, Newman acepta:

```powershell
newman run .\postman\UMBRAL_Sheila.postman_collection.json -e .\postman\UMBRAL_Demo.postman_environment.json
```

Este comando requiere Newman instalado y un entorno privado ya configurado con
tokens válidos. Newman se utilizó como herramienta temporal en `target` durante
la revisión; no se añadieron dependencias Maven ni cambios a la aplicación.

Ver también [GUIA_DEMOSTRACION.md](../GUIA_DEMOSTRACION.md) para compilar y ejecutar
la comprobación automática existente de 17 solicitudes sin copiar tokens.
