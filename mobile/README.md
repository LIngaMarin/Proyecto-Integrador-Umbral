# Umbral · App móvil (Android)

App Android de **Umbral**, el sistema de gestión para academias preuniversitarias. Es una de las tres partes del proyecto integrador:

| Parte | Tecnología | Quién la usa |
|---|---|---|
| **Móvil** (este módulo) | Kotlin + Jetpack Compose + Material 3 | Alumno, docente y apoderado |
| **Web / API REST** | Java + Spring Boot | Campus web y la API que consume el móvil |
| **Administración** | Python + Django (admin) | Personal de la academia |
| **Base de datos** | PostgreSQL | La comparten Spring Boot y Django |

> El celular **no se conecta directo a PostgreSQL**. Pide los datos a la API de Spring Boot con Retrofit, y Spring Boot es quien lee y escribe en PostgreSQL. En el celular, Room solo guarda una **copia** (caché) para usar la app sin internet.

## Perfiles y pantallas

- **Alumno:** inicio con avance semanal, próxima clase y temas débiles. Práctica de la semana y por tema, con solucionario, dificultad y favoritas. Progreso por curso, comparativo anónimo con su sección y horario semanal.
- **Docente:** clases de hoy, alumnos de su sección con búsqueda, y dudas (filtro de pendientes y respuesta).
- **Apoderado:** selector de hijo, asistencia y simulacros con tendencia, avisos de inasistencia y agenda de simulacros.

## Arquitectura

MVVM + Repository, con inyección de dependencias manual.

```
View (Compose)  ──eventos──▶  ViewModel  ──▶  UmbralRepository (interfaz, devuelve Result)
     ▲                            │                  │
     └── collectAsStateWithLifecycle ◀── StateFlow<UiState>   ├─ UmbralRepositoryOffline (Room: copia local)
                                                       │      └─▶ UmbralRepositoryRemote (Retrofit ─▶ Spring Boot ─▶ PostgreSQL)
                                                       │          o DemoRepository (datos en memoria)
AppContainer (UmbralApplication) crea UNA vez: Retrofit, Room, DataStore y el repositorio.
```

**Reglas que cumple el código:**

- Las pantallas **nunca** llaman al repositorio. Solo hablan con su ViewModel.
- Los ViewModels solo conocen la interfaz `UmbralRepository`, y no Retrofit, Room ni `Context`.
- El repositorio **nunca deja escapar una excepción**. Devuelve `Result` y relanza `CancellationException`. Los fallos llegan como `ErrorUmbral` (`SinConexion`, `SesionExpirada`, `Servidor(código)`…).
- Cada pantalla muestra **Cargando, datos, vacío o error con Reintentar**.
- Varias llamadas a la vez usan `async` (inicio del alumno, progreso, datos del hijo).

## Estructura de paquetes

```
com.umbral.app/
├── UmbralApplication.kt, AppContainer.kt   inyección de dependencias
├── MainActivity.kt
├── domain/        modelos, UmbralRepository, SesionRepository, ErrorUmbral, Horario (próxima clase)
├── data/
│   ├── demo/        DemoRepository + cuentas de prueba
│   ├── remote/      UmbralApi (Retrofit), DTOs, TokenInterceptor, UmbralRepositoryRemote
│   ├── local/       SesionDataStore (DataStore), UmbralDatabase (Room) y cache/ (entidades + DAO)
│   └── repository/  UmbralRepositoryOffline (red + Room)
└── ui/
    ├── navegacion/  UmbralApp (NavHost por rol), SesionViewModel, barra de pestañas
    ├── login/       LoginScreen + LoginViewModel
    ├── alumno/  docente/  apoderado/   cada pantalla con su ViewModel en archivo aparte
    ├── common/      componentes, ErrorConReintento, mensajeDe()
    └── theme/
```

## Navegación y sesión

- **Navigation Compose:** hay una ruta por rol (`alumno`, `docente`, `apoderado`) y cada una tiene su propio `NavHost` de pestañas.
- **La sesión se guarda en DataStore** (token, rol y nombre). Por eso sobrevive a rotar la pantalla y a cerrar la app. Si hay sesión guardada, la app entra directo al rol.
- **Al cerrar sesión**, o si el backend responde **401**, se borran la sesión y la caché, y se navega al login con `popUpTo(...) { inclusive = true }`. Los ViewModels del usuario anterior se destruyen.

## Modo demo y API real

En `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_URL", "\"http://10.0.2.2:8080/\"") // 10.0.2.2 = tu PC desde el emulador
buildConfigField("boolean", "USAR_DEMO", "true")                     // false = usa Spring Boot
```

Cuentas de la demo (clave `umbral123`), con botón de "entrar rápido" en el login:

| Correo | Rol |
|---|---|
| camila@umbral.pe | Alumno |
| marcos@umbral.pe | Docente |
| rosa@umbral.pe | Apoderado |

## Contrato de la API (para el backend en Spring Boot)

Todas las rutas, salvo el login, reciben `Authorization: Bearer <token>`. **El backend identifica al usuario por el token**, así que el móvil no envía ids de usuario. Los enums viajan como texto: `ALUMNO`, `DOCENTE`, `APODERADO`, `FACIL`, `MEDIA`, `DIFICIL`, `PRESENTE`, `TARDANZA`, `FALTA`. El horario usa `dia` de 1 (lunes) a 7 (domingo), las horas `"HH:mm"` y las fechas `"aaaa-mm-dd"`.

| Método | Ruta | Cuerpo | Respuesta |
|---|---|---|---|
| POST | `api/auth/login` | `{correo, clave}` | `{token, usuario:{id, nombre, rol, detalle}}`. 401 si la clave es incorrecta |
| GET | `api/horario` | | `[{id, curso, seccion, aula, sede, dia, horaInicio, horaFin, docente}]`. El del alumno o el del docente, según el token |
| GET | `api/practica/semana` | | `[{id, tema, enunciado, opciones[], correcta, solucionario[], dificultad}]` |
| GET | `api/practica/temas` | | `["Fracciones", …]` |
| GET | `api/practica/preguntas?tema=X` | | igual que semana |
| POST | `api/practica/respuestas` | `{preguntaId, opcion, acerto}` | 200/201 |
| GET | `api/practica/marcas` | | `[{preguntaId, dificultad, favorita}]` |
| PUT | `api/practica/preguntas/{id}/dificultad` | `{dificultad}` | 200 |
| PUT | `api/practica/preguntas/{id}/favorita` | `{favorita}` | 200 |
| GET | `api/alumno/progreso` | | `[{curso, completadas, totales}]` |
| GET | `api/alumno/temas-debiles` | | `[{tema, aciertoPct}]` |
| GET | `api/alumno/comparativo` | | `{miPromedio, promedioSeccion}` (anónimo) |
| GET | `api/docente/dudas` | | `[{id, alumno, tema, texto, resuelta, respuesta}]` |
| PUT | `api/docente/dudas/{id}/respuesta` | `{respuesta}` | 200 |
| GET | `api/docente/alumnos` | | `[{id, nombre, seccion, asistenciaPct}]` |
| GET | `api/apoderado/hijos` | | `[{id, nombre, seccion}]` |
| GET | `api/apoderado/hijos/{id}/asistencia` | | `[{fecha, estado}]` |
| GET | `api/apoderado/hijos/{id}/resultados` | | `[{nombre, fecha, puntaje}]` en orden cronológico |
| GET | `api/apoderado/hijos/{id}/simulacros` | | `[{nombre, fecha, hora, aula}]` |
| GET | `api/apoderado/avisos` | | `[{id, mensaje, fecha, leida}]` |
| PUT | `api/apoderado/avisos/{id}/leido` | | 200 |

El backend también debe validar el **rol**. Por ejemplo, un apoderado solo puede pedir datos de **sus** hijos; ocultar botones en el móvil no basta.

## Pruebas

- **Unitarias** (`app/src/test`, se corren con ▶ en Android Studio, sin emulador):
  - `ViewModelsTest`: dudas pendientes, responder, error sin conexión, búsqueda de alumnos, login correcto e incorrecto, tendencia de simulacros.
  - `HorarioTest`: cálculo de la próxima clase y del día de la semana.
  - `DemoRepositoryTest`: datos de la demo y login.
- **De interfaz** (`app/src/androidTest`, en el emulador): `PantallasTest` con `createComposeRule`. Cubre el botón Ingresar, el error de login y Reintentar.

## Pendiente

- Lector de **QR** para asistencia (CameraX + ML Kit). Ya existe la maqueta `ui/screens/attendance/AsistenciaQRScreen.kt`.
- **Foto** en las dudas (`ActivityResultContracts.TakePicture`).
- **Notificaciones push** (Firebase Cloud Messaging) para avisos de inasistencia y simulacros.
- Integrar o eliminar las pantallas y entidades anteriores de `ui/screens`, `model` y `data/local/*Entity` + `data/repository/*Dao`, que hoy no se usan.
