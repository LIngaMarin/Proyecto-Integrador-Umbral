# Manual de Umbral

Guía completa del proyecto: qué es, quién lo hizo, qué contiene, con qué está construido y
cómo probarlo paso a paso. Para una vista rápida de los comandos, consulta también el
[README](README.md).

## Índice

1. [¿Qué es Umbral?](#1-qué-es-umbral)
2. [Integrantes](#2-integrantes)
3. [Qué contiene el repositorio](#3-qué-contiene-el-repositorio)
4. [Herramientas y tecnologías](#4-herramientas-y-tecnologías)
5. [Cómo probar el proyecto](#5-cómo-probar-el-proyecto)
6. [Qué puede hacer cada perfil](#6-qué-puede-hacer-cada-perfil)
7. [API: endpoints del portal del apoderado](#7-api-endpoints-del-portal-del-apoderado)
8. [Arquitectura](#8-arquitectura)
9. [Pruebas automáticas](#9-pruebas-automáticas)
10. [Cómo trabajamos con Git](#10-cómo-trabajamos-con-git)
11. [Estado del proyecto](#11-estado-del-proyecto)
12. [Solución de problemas](#12-solución-de-problemas)

---

## 1. ¿Qué es Umbral?

Umbral es un sistema para gestionar toda la administración de una academia, tanto de cursos
como preuniversitaria. Con él se puede:

- **Administrar la academia:** sedes, aulas, ciclos, cursos, secciones, horarios,
  matrículas y usuarios.
- **Seguir el rendimiento:** promedios por salón después de cada simulacro, temas con más
  errores y comparación entre simulacros.
- **Generar contenido de práctica con inteligencia artificial** (Gemini), siempre revisado
  por el docente antes de llegar a los alumnos.
- **Dar seguimiento a los hijos:** los apoderados consultan la asistencia, los resultados y
  el calendario de simulacros desde un portal web.
- **Practicar desde el celular:** los alumnos y docentes usan una app Android.

Es el proyecto integrador del curso *Construcción y Pruebas de Software* de Tecsup,
Departamento Académico de Tecnología Digital, carrera de Diseño y Desarrollo de Software
(Lima, Perú, 2026-II). Docente del curso: Junior Bestard Aroche.

## 2. Integrantes

| Integrante | Perfil | Qué construyó |
|---|---|---|
| **Lucas Inga Marin** | Administrador | Panel de administración, API Django, portal del apoderado y landing |
| **Sheila Aguirre Tejada** | Usuario (docente y alumno) | Campus web y API Spring Boot (rama `web`) |
| **Dario Maza Medina** | Móvil | App Android (rama `mobiles`) |

## 3. Qué contiene el repositorio

```
Proyecto-Integrador-Umbral/
├── admin/
│   ├── backend/            API Django + DRF (PostgreSQL)  ← puerto 8000
│   │   └── academia/       modelos, vistas, pruebas y datos de demo
│   ├── frontend/           Panel de administración (React)  ← puerto 5173
│   └── portal-apoderado/   Portal web del apoderado (React)  ← puerto 5174
├── landing/                Página de presentación con las 4 puertas de entrada
├── backend/                API Spring Boot del perfil Usuario (Sheila)
├── mobile/                 App Android (Dario)
├── README.md               Comandos rápidos
└── MANUAL.md               Este manual
```

| Carpeta | Para quién es | Dirección en local |
|---|---|---|
| `landing/` | Cualquier visitante | http://localhost:5500 |
| `admin/frontend/` | Administradores y coordinadores | http://localhost:5173 |
| `admin/portal-apoderado/` | Madres, padres y tutores | http://localhost:5174 |
| `admin/backend/` | Es la API que usan los dos portales | http://127.0.0.1:8000/api/ |
| `backend/` y `frontend/` (rama `web`) | Docentes y alumnos (campus web) | http://127.0.0.1:5175 |
| `mobile/` | Alumnos y docentes en Android | Se abre con Android Studio |

## 4. Herramientas y tecnologías

| Parte | Tecnologías |
|---|---|
| API de administración | Python 3.10+, Django 5.2, Django REST Framework, SimpleJWT (inicio de sesión con token), psycopg 3 |
| Base de datos | PostgreSQL |
| Panel y portal del apoderado | React 19, Vite, axios, JavaScript (JSX) |
| Landing | HTML, CSS y JavaScript sin dependencias; fuentes Bricolage Grotesque y Figtree (licencia SIL OFL) |
| Campus web y API del perfil Usuario | Spring Boot (Kotlin), JavaScript con Vite y Bootstrap |
| App móvil | Kotlin, Jetpack Compose, Room (SQLite) |
| Inteligencia artificial | Gemini (`gemini-2.5-flash`), opcional |
| Control de versiones | Git y GitHub: ramas por integrante y merge a `main` |
| Pruebas | `manage.py test` (Django), ESLint (React) |

## 5. Cómo probar el proyecto

### 5.1 Requisitos

- Python 3.10 o superior
- Node.js 20 o superior
- PostgreSQL instalado y **en ejecución**

### 5.2 Paso 1: base de datos y API (terminal 1)

Crea una base de datos vacía en PostgreSQL (por ejemplo `umbral_admin`) y luego:

```bash
cd admin/backend
python3 -m venv ~/.venvs/umbral-admin
source ~/.venvs/umbral-admin/bin/activate
pip install -r requirements.txt
cp .env.example .env          # completa SECRET_KEY y los datos de la base de datos
python manage.py migrate
python manage.py cargar_demo   # datos de demostración (se puede repetir sin duplicar)
python manage.py runserver     # API en http://127.0.0.1:8000
```

Variables del archivo `.env`:

| Variable | Qué es |
|---|---|
| `SECRET_KEY` | Clave secreta de Django (pon cualquier texto largo) |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT` | Conexión a PostgreSQL |
| `GEMINI_API_KEY` | Opcional. Solo para las funciones de IA. Se obtiene gratis en https://aistudio.google.com/apikey |

Para comprobar que la API responde: abre http://127.0.0.1:8000/api/health/ y debe mostrar
`{"status": "ok"}`.

### 5.3 Paso 2: portal del apoderado (terminal 2)

```bash
cd admin/portal-apoderado
npm install
npm run dev                    # http://localhost:5174
```

### 5.4 Paso 3: panel de administración (terminal 3)

```bash
cd admin/frontend
npm install
cp .env.example .env           # VITE_API_URL apunta a la API
npm run dev                    # http://localhost:5173
```

### 5.5 Paso 4: landing (terminal 4)

```bash
cd landing
python3 -m http.server 5500    # http://localhost:5500
```

Cada puerta de la landing muestra si su servicio está **En línea**, **Apagado ahora** o
**Próximamente**. Las direcciones se cambian en `landing/config.js`.

### 5.6 Cuentas de prueba

`cargar_demo` crea una academia de ejemplo (2 sedes, 4 cursos, 12 alumnos, docentes,
apoderados, 2 simulacros por curso y 30 días de asistencia). **Todas las cuentas de demo
usan la contraseña `Umbral2026!`.**

| Perfil | Correo | Dónde entra |
|---|---|---|
| Apoderado (2 hijos: Diego y Valeria) | `rosa.mamani@demo.umbral.pe` | Portal del apoderado |
| Apoderado (1 hijo: Mateo) | `miguel.castillo@demo.umbral.pe` | Portal del apoderado |
| Apoderado (2 hijos: Joaquín y Andrea) | `elena.silva@demo.umbral.pe` | Portal del apoderado |
| Docente | `carlos.ramirez@demo.umbral.pe` | Campus web y app móvil |
| Alumno | `diego.quispe@demo.umbral.pe` | Campus web y app móvil |

**Cuenta de administrador.** Los datos de demo no incluyen administrador. Para crear uno
(con el venv activado, dentro de `admin/backend`):

```bash
python manage.py shell -c "from django.contrib.auth.models import User; from academia.models import Perfil; u = User.objects.create_user('admin@demo.umbral.pe', 'admin@demo.umbral.pe', 'Umbral2026!'); Perfil.objects.create(user=u, rol=Perfil.Rol.ADMIN)"
```

Luego entra al panel (http://localhost:5173) con `admin@demo.umbral.pe` y `Umbral2026!`.
El rol `COORDINADOR` se crea igual, cambiando `Perfil.Rol.ADMIN` por `Perfil.Rol.COORDINADOR`;
ve todo salvo las secciones exclusivas del administrador.

### 5.7 Recorrido sugerido para una demostración

**Como apoderado** (http://localhost:5174, entra con Rosa):

1. Elige a **Diego Quispe Mamani** en las pestañas de los hijos.
2. **Asistencia:** cambia entre últimos 7 días, 30 días y este mes; ve el porcentaje y la
   lista de días.
3. **Resultados de simulacros:** revisa el gráfico y la variación entre el simulacro 1 y el 2.
   Cambia de hijo y los datos cambian.
4. **Calendario de simulacros:** navega con las flechas hasta octubre; los días con
   simulacro están marcados. Haz clic en uno para ver el detalle.
5. **Datos de contacto:** cambia el teléfono y guarda. Prueba un teléfono con letras: debe
   mostrar un error.

**Como administrador** (http://localhost:5173):

1. Revisa el **Panel** de indicadores.
2. Mira **Rendimiento** por salón y **Comparar simulacros**.
3. Entra a **Horarios** y **Matrículas**.
4. En **Apoderados** vincula un apoderado con un alumno.
5. En **Auditoría** verás las acciones que acabas de hacer.

## 6. Qué puede hacer cada perfil

### Administrador y coordinador (panel web, `admin/frontend`)

Panel de indicadores · academias · sedes · aulas · ciclos · días festivos · cursos ·
secciones · matrículas · horarios con detección de conflictos · rendimiento por salón ·
comparación entre simulacros · temas con más errores · simulacros y resultados · usuarios.

Solo el administrador: marca de la academia (logo y colores, con vista previa) ·
restablecer contraseñas · registrar y vincular apoderados · configuración de IA con límite
por docente · carga masiva de alumnos desde Excel · registro de auditoría.

### Apoderado (portal web, `admin/portal-apoderado`)

| HU | Funcionalidad |
|---|---|
| 34 | Consultar la asistencia de cada hijo, con filtro por fechas |
| 35 | Ver los resultados de simulacros por curso, con promedio, gráfico de avance y variación |
| 37 | Actualizar teléfono y correo (con validación) |
| 40 | Ver un calendario mensual con los simulacros programados y su detalle |

Un apoderado **solo ve a sus propios hijos**.

### Docente y alumno (campus web y app móvil)

Práctica de preguntas, banco de preguntas del docente, reportes de rendimiento en PDF,
asistencia, horario y dudas con foto. Están en las ramas `web` (Sheila) y `mobiles` (Dario).

## 7. API: endpoints del portal del apoderado

Todas las rutas empiezan con `http://127.0.0.1:8000/api/` y piden un token de sesión
(`Authorization: Bearer <token>`), excepto el inicio de sesión y la comprobación de estado.

| Método y ruta | Qué hace |
|---|---|
| `POST /auth/login/` | Inicia sesión con `username` y `password`; devuelve los tokens y el rol |
| `GET /health/` | Comprueba que la API está encendida |
| `GET /mis-hijos/` | Lista los hijos del apoderado |
| `GET /mis-hijos/<id>/asistencia/?desde=&hasta=` | Asistencia del hijo (HU 34) |
| `GET /mis-hijos/<id>/simulacros/` | Resultados de simulacros por curso (HU 35) |
| `GET /mis-hijos/<id>/simulacros-programados/?desde=&hasta=` | Simulacros programados (HU 40) |
| `GET` y `PUT /mis-datos/contacto/` | Leer y actualizar el contacto (HU 37) |

Las fechas usan el formato `AAAA-MM-DD` y ambos extremos están incluidos. Si no se indican,
la asistencia usa los últimos 30 días y el calendario va de hoy a 120 días. El rango máximo
es de un año. Las demás rutas (administración) están en `admin/backend/academia/urls.py`.

## 8. Arquitectura

```
Landing (5500) ──enlaza a──▶  Panel de administración (5173)   ┐
                              Portal del apoderado (5174)        ├─ React + Vite + axios
                              Campus web (5175)                  ┘  (Sheila, rama web)
                              App móvil Android (Dario, rama mobiles)
                                         │
                         API REST Django + DRF + JWT (puerto 8000)
                                         │
                                  PostgreSQL (tablas adm_*)
```

- **Cinco roles:** administrador, coordinador, docente, alumno y apoderado. Cada vista de la
  API comprueba el rol (por ejemplo, `EsApoderado` y `EsAdministrador`).
- **Inicio de sesión:** el usuario es su correo. La API entrega un token (JWT) que el portal
  guarda y envía en cada petición. Si el token vence, se vuelve al ingreso.
- **Acceso a datos ajenos:** las consultas del apoderado se hacen sobre sus hijos
  vinculados; si pide a otro alumno, la API responde 404.
- **Cuentas con contraseña temporal:** quien entra con una contraseña temporal debe crear
  una nueva antes de continuar.
- **Marca configurable:** el logo y los colores de la academia se cambian desde el panel y
  se aplican al portal automáticamente.
- **Landing:** es estática; solo lee `landing/config.js` para saber dónde está cada servicio.

## 9. Pruebas automáticas

```bash
# API de administración (con el venv activado y PostgreSQL encendido)
cd admin/backend
python manage.py test                              # todas
python manage.py test academia.test_contacto       # solo una HU

# Revisión de código de los portales
cd admin/portal-apoderado && npx eslint src
cd admin/frontend && npx eslint src
```

Cada historia de usuario tiene su archivo `test_*.py`, que cubre el cálculo, las
validaciones y el control de acceso (un apoderado no puede ver hijos ajenos, un docente no
puede entrar al portal, sin sesión no se entra).

## 10. Cómo trabajamos con Git

- **Ramas:** `main` (la versión estable), `feature/lucas-base-django` (Lucas), `web`
  (Sheila) y `mobiles` (Dario).
- **Flujo:** se trabaja en la rama propia, se hacen commits pequeños y se une a `main` con
  `git merge --no-ff`, para dejar visible cada merge en el historial.
- **Mensajes de commit:** `tipo(parte): qué se hizo (HU n)`. Los tipos son `feat` (nueva
  función), `fix` (corrección), `test` (pruebas), `docs` (documentación) y `style`
  (apariencia).

## 11. Estado del proyecto

**Hecho**
- Panel de administración y API de la academia (sprints anteriores).
- Portal del apoderado: HU 34, 35, 37 y 40, con pruebas.
- Landing con las cuatro puertas de entrada.
- Campus web y API del perfil Usuario (Sheila, rama `web`).

**Pendiente**
- Docker: un solo comando para levantar todo el sistema.
- Unir la rama `web` a `main` e integrar la API de Sheila con la de administración.
- Enlace de descarga del APK para la puerta "App móvil" de la landing.
- Reordenar los archivos de `admin/backend/academia/` por funcionalidad.

## 12. Solución de problemas

| Problema | Causa probable | Qué hacer |
|---|---|---|
| `Connection refused` al puerto 5432 | PostgreSQL apagado | Enciende PostgreSQL y vuelve a ejecutar el comando |
| `No active account found` al iniciar sesión | Faltan los datos de demo | `python manage.py cargar_demo` |
| La landing marca "Apagado ahora" | Ese servicio no está encendido | Levanta el servicio de esa puerta (sección 5) |
| El portal no abre en el puerto 5174 | Otro programa usa el puerto | Cierra el otro programa; el portal no cambia de puerto a propósito |
| Error de CORS en el navegador | El portal corre en un puerto no permitido | Usa los puertos 5173 y 5174, o agrégalos a `CORS_ALLOWED_ORIGINS` en `admin/backend/config/settings.py` |
| Cambié el CSS y no se ve el cambio | Caché del navegador | Recarga con Cmd + Shift + R (o Ctrl + Shift + R) |
| Un alumno no muestra asistencia ni notas | Es un alumno creado a mano, sin datos | Elige a Diego o Valeria, que vienen con los datos de demo |
| Cambié mi correo y no puedo entrar | El correo es también el usuario | Ingresa con el correo nuevo |
| Las funciones de IA no responden | Falta `GEMINI_API_KEY` | Agrégala en `admin/backend/.env` |
