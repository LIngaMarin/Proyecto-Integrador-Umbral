package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.security.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

data class SimulacroCalendarioDTO(val id: Long,val nombre: String,val fecha: LocalDate)

@Service
class CalendarioService(private val jdbc: JdbcTemplate,private val identidad: IdentidadAcademica) {
    /** Valida el vínculo del apoderado y consulta los simulacros activos de las matrículas del alumno. */
    @Transactional(readOnly=true)
    fun consultar(apoderado: Actor,alumnoId: Long,desde: LocalDate,hasta: LocalDate): List<SimulacroCalendarioDTO> {
        validarRango(desde,hasta)
        identidad.exigirHijo(apoderado.id,alumnoId)
        return identidad.consulta {
            // Evita repetir un simulacro si el alumno tiene varias matrículas del mismo curso.
            jdbc.query("""SELECT DISTINCT s.id,s.nombre,s.fecha FROM adm_simulacro s
                JOIN adm_seccion sec ON sec.curso_id=s.curso_id
                JOIN adm_matricula m ON m.seccion_id=sec.id
                WHERE m.alumno_id=? AND m.activa=true AND sec.activa=true AND s.activa=true
                AND s.fecha>=? AND s.fecha<=? ORDER BY s.fecha,s.id""",
                { rs,_ -> SimulacroCalendarioDTO(rs.getLong("id"),rs.getString("nombre"),rs.getDate("fecha").toLocalDate()) },
                alumnoId,desde,hasta)
        }
    }
    companion object {
        /** Rechaza un rango cuya fecha final sea anterior a la inicial. */
        fun validarRango(desde: LocalDate,hasta: LocalDate) {
            if(hasta.isBefore(desde)) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Rango de fechas inválido")
        }
    }
}
