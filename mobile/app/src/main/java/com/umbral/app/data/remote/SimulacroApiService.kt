package com.umbral.app.data.remote

import com.umbral.app.data.local.PreguntasPracticasEntity
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class SimulacroEnvioDto(
    val simulacroId: Long,
    val alumnoId: Long,
    val tiempoEmpleadoSegundos: Int,
    val respuestas: List<RespuestaItemDto>
)

data class RespuestaItemDto(
    val preguntaId: Long,
    val respuestaSeleccionada: String
)

data class SimulacroResultadoResponseDto(
    val id: Long,
    val tituloSimulacro: String,
    val puntajeObtenido: Float,
    val respuestasCorrectas: Int,
    val respuestasIncorrectas: Int,
    val respuestasEnBlanco: Int,
    val mensaje: String
)

interface SimulacroApiService {
    @GET("api/simulacros/{id}/preguntas")
    suspend fun obtenerPreguntasSimulacro(
        @Path("id") simulacroId: Long
    ): List<PreguntasPracticasEntity>

    @POST("api/simulacros/enviar")
    suspend fun enviarSimulacro(
        @Body envio: SimulacroEnvioDto
    ): SimulacroResultadoResponseDto
}
