# Umbral

Plataforma de gestión académica con IA para academias preuniversitarias.
Proyecto integrador de Tecsup, curso Construcción y Pruebas de Software.

Umbral permite administrar una academia (sedes, cursos, horarios, matrículas), generar
contenido de práctica con inteligencia artificial y seguir el rendimiento de los alumnos
desde la web y desde una app móvil.

## Estructura del repositorio

| Carpeta | Perfil | Tecnología |
|---|---|---|
| `admin/backend` | Administrador | Django, Django REST Framework, PostgreSQL |
| `admin/frontend` | Administrador | React, Vite |
| `backend` | Usuario (docente) | Spring Boot, Kotlin |
| `mobile` | Móvil (alumno, apoderado y docente) | Kotlin, Jetpack Compose, Room |

## Panel de administración

### Requisitos
- Python 3.10 o superior
- Node.js 20 o superior
- PostgreSQL en ejecución

### Backend (`admin/backend`)

```bash
cd admin/backend
python3 -m venv ~/.venvs/umbral-admin
source ~/.venvs/umbral-admin/bin/activate
pip install -r requirements.txt
cp .env.example .env        # completa SECRET_KEY y los datos de la base de datos
python manage.py migrate
python manage.py runserver
```

La API queda en `http://127.0.0.1:8000/api/`. Los correos (por ejemplo, las contraseñas
temporales) se imprimen en la terminal mientras no se configure un servidor SMTP.

### Frontend (`admin/frontend`)

```bash
cd admin/frontend
npm install
cp .env.example .env        # VITE_API_URL apunta a la API del backend
npm run dev
```

El panel queda en `http://localhost:5173`.

### Datos de demostración

```bash
cd admin/backend
python manage.py cargar_demo
```

Crea una academia de ejemplo con sedes, cursos, horarios, docentes, alumnos, apoderados y
resultados de simulacros. Todas las cuentas usan el dominio `@demo.umbral.pe` y la
contraseña `Umbral2026!`. El comando se puede ejecutar varias veces sin duplicar datos.

### Pruebas

```bash
cd admin/backend
python manage.py test
```

## Otras partes del proyecto

- La API del perfil Usuario está en `backend/`. Su guía de demostración está en
  `backend/GUIA_DEMOSTRACION.md`.
- La app móvil está en `mobile/` y se abre con Android Studio.

## Integrantes

- Lucas Inga Marin: perfil Administrador
- Sheila Aguirre Tejada: perfil Usuario
- Dario Maza Medina: perfil Móvil
