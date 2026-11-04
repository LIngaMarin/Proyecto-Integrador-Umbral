# UMBRAL

Plataforma académica para academias preuniversitarias.

Esta rama contiene el backend Kotlin/Spring Boot en `backend/`, el portal JavaScript/Vite/Bootstrap en `frontend/` y el cliente Android en `mobile/`. La identidad y las relaciones académicas se leen del esquema administrativo Django compartido, según `backend/CONTRATO_ACADEMICO.md`.

La guía vigente es [CIERRE_SPRINT3_WEB.md](CIERRE_SPRINT3_WEB.md): HU31/32/33/36/41, reportes académicos PDF, inventario de 37 rutas, pruebas, archivos y comandos exactos. [ENTREGA_LOGIN_Y_DISENO.md](ENTREGA_LOGIN_Y_DISENO.md) documenta el acceso convencional y [ENTREGA_WEB.md](ENTREGA_WEB.md) conserva la primera entrega.

El portal conserva JWT y ofrece prácticas, consultas académicas, banco docente, asistencia, reportes y seguimiento familiar. La demo usa H2 con datos ficticios exclusivamente bajo su perfil; PostgreSQL utiliza configuración por entorno. No se incluyen claves ni tokens en el repositorio.

Para iniciar el frontend, consultar [frontend/README.md](frontend/README.md). Para el contrato del backend, consultar [backend/CONTRATO_ACADEMICO.md](backend/CONTRATO_ACADEMICO.md).

El módulo administrativo original documentado en este repositorio corresponde a Lucas Inga.
