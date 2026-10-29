package com.umbral.app.data.repository

import com.umbral.app.data.local.PreguntasPracticasEntity
import com.umbral.app.data.local.RespuestaTemporalEntity
import com.umbral.app.data.local.SimulacroResultadoEntity
import com.umbral.app.data.remote.RespuestaItemDto
import com.umbral.app.data.remote.SimulacroApiService
import com.umbral.app.data.remote.SimulacroEnvioDto
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimulacroRepository(
    private val simulacroDao: SimulacroDao,
    private val preguntaPracticaDao: PreguntaPracticaDao,
    private val respuestaTemporalDao: RespuestaTemporalDao,
    private val apiService: SimulacroApiService
) {

    suspend fun cargarPreguntas(simulacroId: Long): List<PreguntasPracticasEntity> {
        return try {
            val remotePreguntas = apiService.obtenerPreguntasSimulacro(simulacroId)
            remotePreguntas.forEach { preguntaPracticaDao.insertarPregunta(it) }
            remotePreguntas
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun guardarRespuestaTemporal(preguntaId: Long, simulacroId: Long, respuesta: String) {
        respuestaTemporalDao.guardarRespuestaTemporal(
            RespuestaTemporalEntity(
                preguntaId = preguntaId,
                simulacroId = simulacroId,
                respuestaSeleccionada = respuesta
            )
        )
    }

    fun obtenerRespuestasTemporales(simulacroId: Long): Flow<List<RespuestaTemporalEntity>> {
        return respuestaTemporalDao.obtenerRespuestasPorSimulacro(simulacroId)
    }

    suspend fun enviarSimulacro(
        simulacroId: Long,
        alumnoId: Long,
        tiempoSegundos: Int,
        respuestasMap: Map<Long, String>,
        preguntasTotales: List<PreguntasPracticasEntity>
    ): Result<SimulacroResultadoEntity> {
        return try {
            val itemsDto = respuestasMap.map { (pId, resp) ->
                RespuestaItemDto(preguntaId = pId, respuestaSeleccionada = resp)
            }
            val envioDto = SimulacroEnvioDto(
                simulacroId = simulacroId,
                alumnoId = alumnoId,
                tiempoEmpleadoSegundos = tiempoSegundos,
                respuestas = itemsDto
            )

            val resultadoResponse = try {
                apiService.enviarSimulacro(envioDto)
            } catch (e: Exception) {
                // Cálculo offline si la API no responde
                var correctas = 0
                var incorrectas = 0
                var enBlanco = 0

                preguntasTotales.forEach { preg ->
                    val resp = respuestasMap[preg.id]
                    if (resp.isNullOrEmpty()) {
                        enBlanco++
                    } else if (resp.equals(preg.respuestaCorrecta, ignoreCase = true)) {
                        correctas++
                    } else {
                        incorrectas++
                    }
                }

                val total = preguntasTotales.size.coerceAtLeast(1)
                val puntaje = (correctas * 20f) / total
                val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

                val entity = SimulacroResultadoEntity(
                    tituloSimulacro = "Simulacro General",
                    puntajeObtenido = puntaje,
                    respuestasCorrectas = correctas,
                    respuestasIncorrectas = incorrectas,
                    respuestasEnBlanco = enBlanco,
                    fechaRealizacion = fecha
                )
                simulacroDao.guardarResultado(entity)
                respuestaTemporalDao.limpiarRespuestasTemporales(simulacroId)
                return Result.success(entity)
            }

            val fechaFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val entity = SimulacroResultadoEntity(
                tituloSimulacro = resultadoResponse.tituloSimulacro,
                puntajeObtenido = resultadoResponse.puntajeObtenido,
                respuestasCorrectas = resultadoResponse.respuestasCorrectas,
                respuestasIncorrectas = resultadoResponse.respuestasIncorrectas,
                respuestasEnBlanco = resultadoResponse.respuestasEnBlanco,
                fechaRealizacion = fechaFormat
            )
            simulacroDao.guardarResultado(entity)
            respuestaTemporalDao.limpiarRespuestasTemporales(simulacroId)
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
