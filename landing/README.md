# Landing de Umbral

Página de presentación del proyecto: qué es Umbral, cómo funciona, quién lo construyó y
cuatro puertas de entrada a cada parte del sistema. Es HTML, CSS y JavaScript sin
dependencias ni paso de compilación. Las fuentes (Bricolage Grotesque y Figtree, licencia
SIL OFL) están incluidas en `assets/fuentes/`.

## Cómo verla

```bash
cd landing
python3 -m http.server 5500
```

Abre `http://localhost:5500`.

## Conexión con el resto del sistema

Las cuatro puertas leen sus direcciones de `config.js`. Cada una consulta si su servicio
responde y muestra "En línea" o "Apagado ahora". Si una dirección está vacía, la puerta
muestra "Próximamente".

| Puerta | Dirección en `config.js` | Cómo levantarla |
|---|---|---|
| Administración | `admin` (`http://localhost:5173`) | `cd admin/frontend && npm run dev`, con la API en `admin/backend` (`python manage.py runserver`, puerto 8000) |
| Apoderados | `apoderado` (`http://localhost:5174`) | `cd admin/portal-apoderado && npm run dev` |
| Campus web | `campus` (`http://127.0.0.1:5175`) | `npm run dev -- --port 5175` en el frontend de Sheila (el 5173 ya lo usa el panel), con su backend en el puerto 8080 |
| App móvil | `movil` (vacío) | Pendiente: poner aquí el enlace de descarga del APK cuando Dario lo tenga |

Al publicar, basta cambiar esas direcciones por las reales. No hay que tocar el resto del código.

## Estructura

- `index.html`: contenido de todas las secciones.
- `css/`: estilos por sección (`base`, `hero`, `que-es`, `como`, `entradas`, `equipo`).
- `js/`: `hero.js` (puerta animada y cabecera), `como.js` (línea de pasos) y `entradas.js` (estado de las puertas).
- `config.js`: direcciones de cada parte del sistema.
- Íconos de las puertas: SVG en línea dentro de `index.html` (clase `puerta-icono`), con el color de cada perfil definido en `css/entradas.css`.

Las animaciones se desactivan si el sistema operativo pide reducir el movimiento.
