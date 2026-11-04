package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.dto.*
import com.umbral.umbral_backend.repository.ResultadoSimulacroRepository
import com.umbral.umbral_backend.security.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import java.time.*

/** HU41: consulta resultados autorizados y genera el mismo reporte para pantalla y PDF. */
@Service
class RendimientoPdfService(private val resultados: ResultadoSimulacroRepository,
    private val identidad: IdentidadAcademica,private val jdbc:JdbcTemplate,private val json:ObjectMapper,
    private val clock:Clock,private val plantilla:PlantillaRendimientoPdf) {

    /** La sección se autoriza antes de leer notas o metadatos; el filtro nunca amplía ese alcance. */
    @Transactional(readOnly=true)
    fun consultar(docente:Actor,seccionId:Long,simulacroId:Long?=null):ReporteRendimientoDTO {
        if(docente.rol!="DOCENTE")throw ResponseStatusException(HttpStatus.FORBIDDEN)
        identidad.exigirSeccion(docente.id,seccionId)
        if(simulacroId!=null && simulacroId<=0)throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecciona un simulacro válido")
        val nombre=identidad.consulta {
            jdbc.queryForObject("SELECT nombre FROM adm_seccion WHERE id=?",String::class.java,seccionId)
        }?.takeIf{it.isNotBlank()} ?: "Sección #$seccionId"
        val todos=resultados.findBySeccionId(seccionId)
        val seleccion=todos.filter{simulacroId==null || it.simulacroId==simulacroId}
        // Se recuperan solo nombres vinculados a resultados de esta sección; las consultas son parametrizadas.
        val idsSimulacro=todos.map{it.simulacroId}.distinct()
        val simulacros=if(idsSimulacro.isEmpty())emptyList() else identidad.consulta {
            jdbc.query("SELECT id,nombre,fecha FROM adm_simulacro WHERE id IN (${idsSimulacro.joinToString(","){"?"}})",
                {rs,_->SimulacroReporteDTO(rs.getLong("id"),rs.getString("nombre"),rs.getDate("fecha")?.toLocalDate())},*idsSimulacro.toTypedArray())
        }
        val porSimulacro=simulacros.associateBy{it.id}
        val opciones=idsSimulacro.sorted().map{porSimulacro[it] ?: SimulacroReporteDTO(it,null,null)}
        val idsAlumno=seleccion.map{it.alumnoId}.distinct()
        val alumnos=if(idsAlumno.isEmpty())emptyMap<Long,String>() else identidad.consulta {
            jdbc.query("""SELECT p.id,u.email FROM adm_perfil p JOIN auth_user u ON u.id=p.user_id
                WHERE p.rol='ALUMNO' AND p.id IN (${idsAlumno.joinToString(","){"?"}})""",
                {rs,_->rs.getLong("id") to rs.getString("email")},*idsAlumno.toTypedArray()).toMap()
        }
        val filas=seleccion.sortedWith(compareBy({it.alumnoId},{it.simulacroId},{it.id})).map{r->
            if(!r.puntaje.isFinite())throw datosInvalidos()
            val temas=try {
                val node=json.readTree(r.temasIncorrectosJson)
                if(!node.isArray || node.any{!it.isString})throw datosInvalidos()
                buildList<String>{node.forEach{value->val tema=value.asString().trim();if(tema.isNotEmpty())add(tema)}}
            }catch(ex:ResponseStatusException){throw ex}catch(_:Exception){throw datosInvalidos()}
            FilaRendimientoDTO(r.id,r.alumnoId,alumnos[r.alumnoId]?.takeIf{it.isNotBlank()},r.simulacroId,
                porSimulacro[r.simulacroId]?.nombre,r.puntaje,temas)
        }
        val notas=filas.map{it.puntaje}
        // Un puntaje puede usar distintas escalas: no se inventan porcentajes ni umbrales de aprobación.
        val resumen=IndicadoresReporteDTO(filas.size,filas.map{it.alumnoId}.distinct().size,
            if(notas.isEmpty())null else notas.average(),notas.minOrNull(),notas.maxOrNull())
        val temas=filas.flatMap{it.temasPorReforzar}.groupingBy{it}.eachCount().map{TemaDebilDTO(it.key,it.value)}
            .sortedWith(compareByDescending<TemaDebilDTO>{it.cantidadErrores}.thenBy{it.tema})
        return ReporteRendimientoDTO(seccionId,nombre,docente.email,OffsetDateTime.now(clock.withZone(ZoneId.of("America/Lima"))),
            simulacroId,resumen,opciones,filas,temas)
    }

    /** Conserva la ruta PDF existente y convierte errores de generación en una respuesta sin archivo falso. */
    @Transactional(readOnly=true)
    fun generar(docente:Actor,seccionId:Long,simulacroId:Long?=null):ByteArray {
        val reporte=consultar(docente,seccionId,simulacroId)
        return try{plantilla.generar(reporte)}catch(ex:Exception){
            LoggerFactory.getLogger(javaClass).error("No se pudo generar reporte de sección {}: {}",seccionId,ex.javaClass.simpleName)
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo generar el reporte. Vuelve a intentarlo")
        }
    }
    private fun datosInvalidos()=ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
        "Hay resultados académicos que requieren revisión antes de generar el reporte")
}
