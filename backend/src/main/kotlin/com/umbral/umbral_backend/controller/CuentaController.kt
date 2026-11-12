package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.security.IdentidadAcademica
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** Contexto del portal: obtiene el rol de la base, nunca del navegador. */
@RestController
class CuentaController(private val identidad: IdentidadAcademica, private val jdbc: JdbcTemplate) {
    /** Devuelve solo secciones asignadas o hijos vinculados del usuario autenticado. */
    @GetMapping("/api/cuenta")
    fun cuenta(@AuthenticationPrincipal jwt: Jwt): Map<String, Any> {
        val actor = try { identidad.actor(jwt, "DOCENTE") }
        catch (ex: org.springframework.web.server.ResponseStatusException) {
            if (ex.statusCode.value() != 403) throw ex
            try { identidad.actor(jwt, "ALUMNO") }
            catch (otro: org.springframework.web.server.ResponseStatusException) {
                if (otro.statusCode.value() != 403) throw otro
                identidad.actor(jwt, "APODERADO")
            }
        }
        val secciones = identidad.consulta {
            when (actor.rol) {
                "DOCENTE" -> jdbc.queryForList("""SELECT DISTINCT s.id,s.nombre FROM adm_seccion s
                    JOIN adm_horario h ON h.seccion_id=s.id WHERE h.docente_id=? AND h.activa=true AND s.activa=true""", actor.id)
                "ALUMNO" -> jdbc.queryForList("""SELECT DISTINCT s.id,s.nombre,h.docente_id FROM adm_seccion s
                    JOIN adm_matricula m ON m.seccion_id=s.id JOIN adm_horario h ON h.seccion_id=s.id
                    WHERE m.alumno_id=? AND m.activa=true AND s.activa=true AND h.activa=true""", actor.id)
                else -> emptyList()
            }
        }
        val hijos = if (actor.rol == "APODERADO") identidad.consulta {
            jdbc.queryForList("""SELECT p.id,u.email FROM adm_apoderado a
                JOIN adm_apoderado_hijos h ON h.apoderado_id=a.id JOIN adm_perfil p ON p.id=h.perfil_id
                JOIN auth_user u ON u.id=p.user_id WHERE a.perfil_id=? AND p.rol='ALUMNO' AND u.is_active=true""", actor.id)
        } else emptyList()
        return mapOf("id" to actor.id, "rol" to actor.rol, "email" to actor.email,
            "secciones" to secciones.map { row -> row.mapKeys { it.key.lowercase() } },
            "hijos" to hijos.map { row -> row.mapKeys { it.key.lowercase() } })
    }
}
