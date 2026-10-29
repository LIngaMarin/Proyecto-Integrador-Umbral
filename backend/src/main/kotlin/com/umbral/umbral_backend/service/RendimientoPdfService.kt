package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.repository.ResultadoSimulacroRepository
import com.umbral.umbral_backend.security.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Service
class RendimientoPdfService(private val resultados: ResultadoSimulacroRepository,
    private val simulacroService: SimulacroService,private val identidad: IdentidadAcademica,private val pdf: DocumentoPdf) {
    /** Genera el informe con resultados y temas débiles de una sección autorizada para el docente. */
    @Transactional(readOnly=true)
    fun generar(docente: Actor,seccionId: Long): ByteArray {
        identidad.exigirSeccion(docente.id,seccionId)
        val datos=resultados.findBySeccionId(seccionId)
        val temas=simulacroService.calcularTemasDebiles(seccionId)
        fun numero(n: Double)=String.format(Locale.ROOT,"%.2f",n)
        return pdf.generar(buildList {
            add("UMBRAL - Rendimiento por sección $seccionId")
            add("Resultados registrados: ${datos.size}")
            add("Alumnos con resultados: ${datos.map { it.alumnoId }.distinct().size}")
            add("Promedio por resultado: ${if(datos.isEmpty()) "Sin datos" else numero(datos.map { it.puntaje }.average())}")
            add("")
            add("Detalle: alumno | simulacro | puntaje")
            datos.sortedWith(compareBy({it.alumnoId},{it.simulacroId},{it.id})).forEach {
                add("${it.alumnoId} | ${it.simulacroId} | ${numero(it.puntaje)}")
            }
            if(datos.isEmpty()) add("La sección no tiene resultados registrados.")
            add("")
            add("Temas débiles: tema | errores registrados")
            temas.forEach { add("${it.tema} | ${it.cantidadErrores}") }
        })
    }
}
