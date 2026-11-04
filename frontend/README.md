# UMBRAL · Campus preuniversitario

Portal JavaScript, Vite 7 y Bootstrap 5 conectado a Kotlin/Spring Boot. Incluye portada pública con carrusel, login convencional, catálogo público y paneles de alumno, docente y apoderado.

La guía vigente está en [ENTREGA_LOGIN_Y_DISENO.md](../ENTREGA_LOGIN_Y_DISENO.md): inventario completo de endpoints, seguridad, archivos, historias, pruebas, ejecución y límites conocidos. Sustituye el acceso manual con tokens.

## Ejecutar

Configurar JAVA_HOME con JDK >=25 y generar primero el JAR desde backend con `mvnw.cmd -o -B -Pdemo package`. Dentro de frontend:

```powershell
npm.cmd ci
npm.cmd run demo
```

En otra terminal, dentro de frontend:

```powershell
npm.cmd run dev
```

Abrir `http://localhost:5173/`. Iniciar sesión con `alumno.demo`, `docente.demo` o `apoderado.demo` y la contraseña demo entregada fuera de Git. También se acepta su correo `example.invalid`. No se guardan contraseñas en JavaScript ni se muestran tokens.

El backend usa 8080 por defecto. Si está ocupado, iniciar demo con `UMBRAL_DEMO_PORT=8081` y Vite con `UMBRAL_API_TARGET=http://127.0.0.1:8081`. Las variables se configuran en las terminales respectivas; hay comandos completos en la guía principal.

## Verificar

```powershell
npm.cmd test
npm.cmd run build
npm.cmd run test:browser
npm.cmd run test:e2e
```

Las suites de navegador se ejecutan secuencialmente. Arrancan servidores aislados, generan una contraseña aleatoria de pruebas en memoria y usan Edge instalado, o el ejecutable indicado en `UMBRAL_BROWSER`. Las trazas están desactivadas para no capturar credenciales; los artefactos de prueba están ignorados por Git.

## Organización

- `src/landing.js`: portada, carrusel, menú móvil y movimiento reducido.
- `src/icons.js`, `src/style.css`: iconos, apariencia, hover, foco y responsive.
- `src/main.js`: login y paneles por rol; prácticas, historial persistido, calendario, consultas, banco, asistencia y reportes.
- `src/api.js`: gestión automática del access token, timeout, errores, JSON y multipart.
- `src/componentes.js`, `practicas.js`, `dudas.js`: contratos y componentes anteriores preservados; validación compartida.
- `src/sesion.js`: adaptador de la ruta anterior.
- `public/images`: fotografías educativas WebP locales y sus fuentes.
- `scripts-demo.mjs`, `vite.config.js`: inicio local sin tokens visibles y proxy configurable.
- `tests`: regresión, seguridad del cliente y flujo completo con backend real.

La compilación genera `dist`; producción requiere servidor HTTPS con proxy `/api`. Se conservan roles, matrícula y vínculos. No se ha validado PostgreSQL real ni se ofrece IA semántica o envío externo de avisos de asistencia.
