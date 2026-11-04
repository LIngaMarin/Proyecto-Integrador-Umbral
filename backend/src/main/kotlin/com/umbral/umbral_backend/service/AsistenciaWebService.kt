package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.security.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.*

/** Registro real de clases del portal y notificaciones internas al cerrar su asistencia. */
@Service
class AsistenciaWebService(private val jdbc: JdbcTemplate, private val identidad: IdentidadAcademica, private val clock: Clock) {
    /** Conserva fechas de clase en hora local académica; evita que JDBC les agregue una zona UTC ficticia. */
    private fun filas(sql: String, vararg args: Any) = jdbc.queryForList(sql, *args).map { row ->
        row.mapKeys { it.key.lowercase() }.mapValues { (_, value) ->
            if (value is java.sql.Timestamp) value.toLocalDateTime() else value
        }
    }

    /** Lista exclusivamente las clases del docente autenticado. */
    fun clases(actor: Actor) = filas("SELECT * FROM clases_web WHERE docente_id=? ORDER BY fin DESC,id DESC", actor.id)

    /** Exige una sección asignada y fecha futura para no crear ausencias retrospectivas. */
    @Transactional
    fun crear(actor: Actor, seccionId: Long, titulo: String, fin: LocalDateTime): Map<String, Any?> {
        identidad.exigirSeccion(actor.id, seccionId)
        if (!fin.isAfter(LocalDateTime.now(clock)) || fin.isAfter(LocalDateTime.now(clock).plusYears(1)))
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique una fecha futura dentro del próximo año")
        val key = GeneratedKeyHolder()
        jdbc.update({ connection -> connection.prepareStatement(
            "INSERT INTO clases_web(docente_id,seccion_id,titulo,fin,cerrada) VALUES(?,?,?,?,false)", arrayOf("id")
        ).apply { setLong(1,actor.id); setLong(2,seccionId); setString(3,titulo.trim()); setObject(4,fin) } }, key)
        return filas("SELECT * FROM clases_web WHERE id=?", key.key!!.toLong()).single()
    }

    /** Bloquea la clase antes de modificarla para serializar el cierre con el registro de presentes. */
    private fun clase(actor: Actor, id: Long): Map<String, Any?> {
        val c = filas("SELECT * FROM clases_web WHERE id=? FOR UPDATE", id).singleOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if ((c["docente_id"] as Number).toLong() != actor.id) throw ResponseStatusException(HttpStatus.FORBIDDEN)
        identidad.exigirSeccion(actor.id,(c["seccion_id"] as Number).toLong())
        return c
    }

    /** Obtiene los alumnos matriculados y su presencia; nunca expone otra sección. */
    @Transactional
    fun alumnos(actor: Actor, id: Long): List<Map<String, Any?>> {
        val c = clase(actor,id)
        return identidad.consulta {
            filas("""SELECT p.id,u.email,CASE WHEN a.id IS NULL THEN false ELSE true END AS presente
                FROM adm_matricula m JOIN adm_perfil p ON p.id=m.alumno_id JOIN auth_user u ON u.id=p.user_id
                LEFT JOIN asistencias_web a ON a.alumno_id=p.id AND a.clase_id=?
                WHERE m.seccion_id=? AND m.activa=true AND p.rol='ALUMNO' AND u.is_active=true ORDER BY p.id""",
                id,(c["seccion_id"] as Number).toLong()).distinctBy { it["id"] }
        }
    }

    /** Registrar presencia repetida es idempotente; la clase cerrada conserva su asistencia. */
    @Transactional
    fun presente(actor: Actor, id: Long, alumnoId: Long) {
        val c = clase(actor,id)
        if (c["cerrada"] == true) throw ResponseStatusException(HttpStatus.CONFLICT,"La clase ya está cerrada")
        identidad.exigirMatricula(alumnoId,(c["seccion_id"] as Number).toLong())
        jdbc.update("""INSERT INTO asistencias_web(clase_id,alumno_id) SELECT ?,?
            WHERE NOT EXISTS(SELECT 1 FROM asistencias_web WHERE clase_id=? AND alumno_id=?)""", id,alumnoId,id,alumnoId)
    }

    /** Cierra solo clases finalizadas y notifica a apoderados vinculados de alumnos sin presencia. */
    @Transactional
    fun cerrar(actor: Actor, id: Long) {
        val c = clase(actor,id)
        if (c["cerrada"] == true) return
        val fin = c["fin"] as LocalDateTime
        if (fin.isAfter(LocalDateTime.now(clock))) throw ResponseStatusException(HttpStatus.CONFLICT,"La clase todavía no ha finalizado")
        identidad.consulta {
            jdbc.update("""INSERT INTO notificaciones_academicas(clase_id,alumno_id,apoderado_id,descripcion,fecha)
                SELECT DISTINCT ?,m.alumno_id,ap.perfil_id,?,? FROM adm_matricula m
                JOIN adm_perfil p ON p.id=m.alumno_id JOIN auth_user u ON u.id=p.user_id
                JOIN adm_apoderado_hijos h ON h.perfil_id=m.alumno_id JOIN adm_apoderado ap ON ap.id=h.apoderado_id
                JOIN adm_perfil pp ON pp.id=ap.perfil_id JOIN auth_user pu ON pu.id=pp.user_id
                WHERE m.seccion_id=? AND m.activa=true AND p.rol='ALUMNO' AND u.is_active=true
                AND pp.rol='APODERADO' AND pu.is_active=true
                AND NOT EXISTS(SELECT 1 FROM asistencias_web a WHERE a.clase_id=? AND a.alumno_id=m.alumno_id)""",
                id,"No se registró asistencia a la clase: ${c["titulo"]}",LocalDateTime.now(clock),c["seccion_id"],id)
        }
        jdbc.update("UPDATE clases_web SET cerrada=true WHERE id=?",id)
    }

    /** El vínculo se vuelve a validar al leer: un apoderado desvinculado pierde acceso al aviso. */
    fun notificaciones(actor: Actor, alumnoId: Long): List<Map<String, Any?>> {
        identidad.exigirHijo(actor.id,alumnoId)
        return filas("SELECT * FROM notificaciones_academicas WHERE apoderado_id=? AND alumno_id=? ORDER BY fecha DESC,id DESC",actor.id,alumnoId)
    }

    /** Candidatos limitados para que cada pasada automática sea acotada. */
    fun vencidas() = filas("SELECT id,docente_id FROM clases_web WHERE cerrada=false AND fin<=? ORDER BY fin LIMIT 100",LocalDateTime.now(clock))
}
