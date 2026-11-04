package com.umbral.umbral_backend.dto

import java.time.LocalDate
import java.time.OffsetDateTime

/** Indicadores calculados de los registros seleccionados; null distingue ausencia de notas de un cero real. */
data class IndicadoresReporteDTO(val resultados:Int,val alumnos:Int,val promedio:Double?,val minimo:Double?,val maximo:Double?)

/** Datos de simulacro disponibles en el contrato administrativo; nunca se inventan nombre ni fecha. */
data class SimulacroReporteDTO(val id:Long,val nombre:String?,val fecha:LocalDate?)

/** Fila académica autorizada; el alumno se identifica por correo cuando existe ese dato. */
data class FilaRendimientoDTO(val id:Long,val alumnoId:Long,val alumno:String?,val simulacroId:Long,
    val simulacro:String?,val puntaje:Double,val temasPorReforzar:List<String>)

/** Mismo contenido para la consulta web y su exportación: solo cambian la representación y la fecha de lectura. */
data class ReporteRendimientoDTO(val seccionId:Long,val seccionNombre:String,val docenteEmail:String,
    val generadoEn:OffsetDateTime,val simulacroId:Long?,val resumen:IndicadoresReporteDTO,
    val simulacros:List<SimulacroReporteDTO>,val resultados:List<FilaRendimientoDTO>,val temas:List<TemaDebilDTO>)
