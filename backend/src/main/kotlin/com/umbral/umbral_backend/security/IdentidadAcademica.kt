package com.umbral.umbral_backend.security

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.dao.DataAccessException

/** Identidad académica verificada; id corresponde al perfil de Django. */
data class Actor(val id: Long, val rol: String, val email: String)

/** Lecturas del esquema Django; Flyway de Kotlin no administra ni modifica estas tablas. */
@Service
class IdentidadAcademica(private val jdbc: JdbcTemplate) {
    /** Obtiene el perfil del usuario activo y comprueba el rol requerido por la operación. */
    fun actor(jwt: Jwt, rol: String): Actor {
        val userId = jwt.getClaimAsString("user_id")?.toLongOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val actores = consulta {
            jdbc.query("""SELECT p.id, p.rol, u.email FROM adm_perfil p
                JOIN auth_user u ON u.id=p.user_id WHERE u.id=? AND u.is_active=true""",
                { rs, _ -> Actor(rs.getLong("id"), rs.getString("rol"), rs.getString("email")) }, userId)
        }
        val actor = actores.singleOrNull() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        if (actor.rol != rol) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        return actor
    }

    /** Comprueba que el docente activo tenga un horario vigente en la sección activa. */
    fun exigirSeccion(docenteId: Long, seccionId: Long) {
        exigir("""SELECT COUNT(*) FROM adm_horario h JOIN adm_seccion s ON s.id=h.seccion_id
            JOIN adm_perfil p ON p.id=h.docente_id JOIN auth_user u ON u.id=p.user_id
            WHERE h.docente_id=? AND h.seccion_id=? AND h.activa=true AND s.activa=true
            AND p.rol='DOCENTE' AND u.is_active=true""", docenteId, seccionId)
    }

    /** Exige una matrícula activa del alumno en una sección atendida por el docente. */
    fun exigirPregunta(alumnoId: Long, docenteId: Long) {
        exigir("""SELECT COUNT(*) FROM adm_matricula m JOIN adm_horario h ON h.seccion_id=m.seccion_id
            JOIN adm_seccion s ON s.id=m.seccion_id JOIN adm_perfil p ON p.id=h.docente_id
            JOIN auth_user u ON u.id=p.user_id WHERE m.alumno_id=? AND h.docente_id=?
            AND m.activa=true AND h.activa=true AND s.activa=true AND p.rol='DOCENTE' AND u.is_active=true""", alumnoId, docenteId)
    }

    /** Comprueba que el alumno esté matriculado en la sección y ambos registros sigan activos. */
    fun exigirMatricula(alumnoId: Long, seccionId: Long) {
        exigir("""SELECT COUNT(*) FROM adm_matricula m JOIN adm_seccion s ON s.id=m.seccion_id
            WHERE m.alumno_id=? AND m.seccion_id=? AND m.activa=true AND s.activa=true""", alumnoId, seccionId)
    }

    /** Valida el vínculo del apoderado con un alumno cuyo usuario esté activo. */
    fun exigirHijo(apoderadoId: Long, alumnoId: Long) {
        exigir("""SELECT COUNT(*) FROM adm_apoderado a JOIN adm_apoderado_hijos h ON h.apoderado_id=a.id
            JOIN adm_perfil p ON p.id=h.perfil_id JOIN auth_user u ON u.id=p.user_id
            WHERE a.perfil_id=? AND h.perfil_id=? AND p.rol='ALUMNO' AND u.is_active=true""", apoderadoId, alumnoId)
    }

    /** Consulta los docentes activos para preparar los resúmenes semanales. */
    fun docentes(): List<Actor> = consulta {
        jdbc.query("""SELECT p.id,p.rol,u.email FROM adm_perfil p JOIN auth_user u ON u.id=p.user_id
            WHERE p.rol='DOCENTE' AND u.is_active=true""",
            { rs, _ -> Actor(rs.getLong("id"), rs.getString("rol"), rs.getString("email")) })
    }

    private fun exigir(sql: String, vararg ids: Long) {
        if (consulta { jdbc.queryForObject(sql, Long::class.java, *ids.toTypedArray()) } == 0L)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a este recurso")
    }

    /** Devuelve 503 si el esquema académico no está disponible, sin conceder permisos por defecto. */
    fun <T> consulta(block: () -> T): T = try { block() } catch (ex: DataAccessException) {
        throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El esquema académico de Django no está disponible", ex)
    }
}
