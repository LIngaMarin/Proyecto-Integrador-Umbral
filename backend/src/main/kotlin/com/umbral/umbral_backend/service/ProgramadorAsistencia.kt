package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.security.IdentidadAcademica
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/** Detecta clases vencidas cada minuto; el bloqueo transaccional impide avisos duplicados. */
@Component
class ProgramadorAsistencia(private val service: AsistenciaWebService,private val identidad: IdentidadAcademica) {
    @Scheduled(fixedDelayString="\${umbral.asistencia.intervalo-ms:60000}", initialDelayString="\${umbral.asistencia.intervalo-ms:60000}")
    fun ejecutar() {
        val docentes = identidad.docentes().associateBy { it.id }
        service.vencidas().forEach { clase ->
            val actor = docentes[(clase["docente_id"] as Number).toLong()] ?: return@forEach
            try { service.cerrar(actor,(clase["id"] as Number).toLong()) }
            catch (ex: Exception) { LoggerFactory.getLogger(javaClass).warn("No se pudo cerrar clase {}: {}",clase["id"],ex.javaClass.simpleName) }
        }
    }
}
