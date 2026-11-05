# Portal del apoderado

Portal web para que los apoderados sigan a sus hijos: asistencia, resultados de simulacros,
calendario y datos de contacto. Usa la API Django del panel de administración (`../backend`),
así que primero hay que tenerla corriendo en http://127.0.0.1:8000.

## Cómo correrlo

```bash
cd admin/portal-apoderado
npm install
npm run dev        # http://localhost:5174
```

Por defecto apunta a `http://127.0.0.1:8000/api`. Para cambiarlo, copia `.env.example` a `.env`
y edita `VITE_API_URL`.

## Cuenta de prueba

Después de `python manage.py cargar_demo` (ver el README de la raíz) puedes entrar con
`rosa.mamani@demo.umbral.pe` y la contraseña `Umbral2026!`. Solo entran cuentas con rol
Apoderado; las demás reciben un aviso.
