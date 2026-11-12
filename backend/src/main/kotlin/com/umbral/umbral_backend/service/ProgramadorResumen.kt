package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.repository.ResumenSemanalRepository
import com.umbral.umbral_backend.security.IdentidadAcademica
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.*
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.*
import org.springframework.stereotype.Component
import java.time.*

@Configuration @EnableScheduling
class RelojResumenConfig {
    /** Configura el reloj usado para calcular las semanas del resumen docente. */
    @Bean fun clock(env: Environment): Clock = Clock.system(ZoneId.of(env.getProperty("umbral.resumen.zona","America/Lima")))
}

@Component
class ProgramadorResumen(private val identidad: IdentidadAcademica,private val estadisticas: EstadisticasSemanaService,
    private val resumenes: ResumenSemanalService,private val repository: ResumenSemanalRepository,private val env: Environment) {
    private val logger=LoggerFactory.getLogger(javaClass)

    /** Prepara los resúmenes de docentes activos y procesa los envíos cuando la tarea está habilitada. */
    @Scheduled(cron="\${umbral.resumen.cron:0 0 8 * * MON}",zone="\${umbral.resumen.zona:America/Lima}")
    fun ejecutar() {
        if(!env.getProperty("umbral.resumen.enabled",Boolean::class.java,false)) return
        val semana=estadisticas.semanaAnterior()
        identidad.docentes().forEach { docente ->
            try { resumenes.preparar(docente,semana) }
            catch(ex: Exception) { logger.warn("No se pudo preparar resumen docente {}: {}",docente.id,ex.javaClass.simpleName) }
        }
        procesarPendientes()
    }

    @Scheduled(fixedDelayString="\${umbral.resumen.reintento-ms:900000}",initialDelayString="\${umbral.resumen.reintento-ms:900000}")
    fun reintentar() {
        // Recupera también el resumen de la última semana si la aplicación estaba apagada el lunes.
        ejecutar()
    }

    /** Reintenta resúmenes pendientes o fallidos que no hayan alcanzado el límite configurado. */
    private fun procesarPendientes() {
        val max=env.getProperty("umbral.resumen.max-intentos",Int::class.java,3)
        repository.findTop100ByEstadoInAndIntentosLessThanOrderByIdAsc(listOf("PENDIENTE","ERROR"),max).forEach { resumen ->
            try { resumenes.enviar(resumen.id) }
            catch(ex: Exception) { logger.warn("No se pudo procesar resumen {}: {}",resumen.id,ex.javaClass.simpleName) }
        }
    }
}
