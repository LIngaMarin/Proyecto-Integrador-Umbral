# UMBRAL · Módulo web

Frontend en JavaScript, Vite y Bootstrap 5. Conserva `frontend/.gitkeep` y no modifica
fuentes del backend ni del módulo móvil. La carpeta estaba vacía al comenzar.

## Ejecutar el frontend

Requiere Node.js 20.19+ o 22.12+; verificado con Node.js 24.19.0.

```powershell
cd C:\PIUMBRAL\frontend
npm ci
npm run dev
```

Abre la dirección indicada por Vite, normalmente `http://127.0.0.1:5173`.
No abras `index.html` directamente. Bootstrap CSS y JavaScript se incluyen en el
bundle local, sin depender de un CDN. El menú utiliza el componente
[Collapse de Bootstrap](https://getbootstrap.com/docs/5.3/components/navbar/).

```powershell
npm run build
npm run preview
```

La previsualización normalmente usa `http://127.0.0.1:4173`. Tanto desarrollo como
previsualización envían `/api` a `http://127.0.0.1:8080` mediante proxy. Para publicar
`dist`, el servidor de destino deberá proporcionar ese mismo proxy hacia Spring
Boot por HTTPS; `dist` por sí solo no configura un servidor de API.

## Iniciar Spring Boot en demostración H2

En otra terminal, desde `backend`, con JDK 25 o superior:

```powershell
cd C:\PIUMBRAL\backend
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot'
.\mvnw.cmd -B -Pdemo -DskipTests package
& "$env:JAVA_HOME\bin\java.exe" -jar .\target\umbral-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=8080
```

Adapta la ruta del JDK a tu equipo. Si Maven presenta errores de certificados,
consulta la [guía existente del backend](../backend/GUIA_DEMOSTRACION.md).
`-DskipTests` solamente empaqueta; no significa que se hayan ejecutado sus pruebas.

H2 utiliza **datos ficticios de demostración**, devueltos realmente por la API.
Se pierden al detener esa instancia. En una demo recién iniciada puedes buscar
`Algebra` con dificultad `FACIL` o `INTERMEDIO`. La base definitiva continúa
pendiente y no se configuró PostgreSQL desde el módulo web.

## JWT e integración pendiente

Kotlin no expone login. Obtén un **access token** del proveedor administrativo
autorizado y colócalo en «Acceso de alumno», sin el prefijo `Bearer`.
El servidor debe tener configurado `UMBRAL_JWT_SECRET` con la misma clave del
emisor y las relaciones académicas correspondientes. No incluyas claves o tokens
en Git. El arranque demo anterior permite consultar el catálogo público, pero no
crea tokens ni configura un proveedor de identidad.

El frontend conserva el token únicamente en memoria y lo envía solo a las rutas
protegidas. La comprobación de estructura local **no autentica** al usuario:
Spring Boot valida firma, vencimiento, identidad, rol y matrícula. Cambiar o
eliminar el token limpia los resultados temporales; recargar también los elimina.
No se desactiva ninguna protección del backend.

## Funcionalidades y rutas reales

| Interfaz | Petición | Acceso |
| --- | --- | --- |
| Catálogo con filtros | `GET /api/practicas/preguntas?tema=...&dificultad=...` | Público |
| Enviar respuesta | `POST /api/practicas/respuestas` | Alumno JWT |
| Solucionario aprobado | `GET /api/practicas/preguntas/{id}/solucionario` | Alumno, intento previo |
| Consultar dudas | `GET /api/alumno/dudas?estado=...` | Alumno JWT |
| Crear duda | `POST /api/alumno/dudas` | Alumno JWT |
| Respuesta docente | `GET /api/alumno/dudas/{id}/respuesta` | Alumno propietario |
| Resolver duda | `PUT /api/alumno/dudas/{id}/resolver` | Alumno, respuesta previa |

Los filtros de dificultad usan `FACIL`, `INTERMEDIO`, `DIFICIL`; los estados de
duda son `PENDIENTE` y `RESUELTA`. El tema es texto libre porque no hay un endpoint
público de temas. Los IDs de docente y sección deben obtenerse de la academia:
no se inventa un listado. La creación de dudas exige tema, texto e IDs positivos.

El dashboard presenta accesos a módulos, sin estadísticas inventadas. Resultados
muestra exclusivamente intentos confirmados durante la sesión, no un historial
global: no existe un GET de historial de prácticas en los controladores revisados.
La solución aprobada puede no existir y la demo inicia sin solucionarios.

## Pruebas de navegador

Primero genera el JAR H2 con el comando anterior y detén las instancias que ocupen
8080 y 5173. Las pruebas arrancan y detienen sus propios servidores; rechazan
reutilizar uno existente para evitar apuntar a datos ajenos.

```powershell
cd C:\PIUMBRAL\frontend
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot'
npx playwright install chromium
npm test
```

La clave y los JWT de pruebas se generan en memoria y solo funcionan contra esa
instancia H2. El recorrido usa los alumnos/docentes ficticios de los fixtures.
Los errores HTTP y de red se simulan exclusivamente en las pruebas; las pantallas
de producción no contienen datos de ejemplo ni respuestas de respaldo.

Verificado el 29/10/2026: compilación Vite y 11 pruebas Chromium aprobadas. Incluyen
375/768/1440 px, Bootstrap, menú hamburguesa, navegación sin errores de consola,
filtros contra Spring Boot, estado vacío, validaciones, JWT, respuesta correcta,
resultados, duda, respuesta docente y resolución, HTTP 400/401/403/404/409/500 y
error de red con recuperación del botón. Se comprobó el estado de ausencia de
solucionario, no la presentación de uno aprobado con contenido real.

No se afirma compatibilidad comprobada con Safari/Firefox, integración con la
base definitiva ni autenticación con el proveedor administrativo desplegado.

## Organización y comentarios

- `index.html`: estructura, menú responsive, región principal y footer.
- `src/main.js`: inicio, tarjetas y navegación por hash.
- `src/styles.css`: paleta, tarjetas, estados de foco y ajustes responsive.
- `src/api.js`: HTTP, timeout, JWT y errores del servidor.
- `src/componentes.js`: escape de datos, validaciones y estados de carga.
- `src/practicas.js`: catálogo, envío de respuestas, solucionario y resultados.
- `src/dudas.js`: registro, consulta, respuesta y resolución de dudas.
- `src/sesion.js`: manejo temporal del token.
- `vite.config.js`: proxy local, sin cambios de CORS en Kotlin.
- `playwright.config.js` y `tests/web.spec.js`: pruebas con navegador y H2.

Los comentarios explican en español las secciones HTML, los componentes, la
navegación, los contratos HTTP, las validaciones, los errores y los estilos
principales. Los archivos JSON de dependencias se mantienen en su formato estándar,
que no admite comentarios. Las dependencias instaladas y los bundles generados
no se comentan ni se incorporan al repositorio.
