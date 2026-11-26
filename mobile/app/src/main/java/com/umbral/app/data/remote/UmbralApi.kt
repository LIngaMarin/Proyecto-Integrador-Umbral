package com.umbral.app.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints de la API REST de Spring Boot (ver README: "Contrato de la API").
 * Todas las rutas, salvo el login, necesitan el encabezado Authorization: Bearer <token>,
 * que agrega TokenInterceptor. El backend sabe quien es el usuario por ese token.
 * Las rutas no empiezan con "/" para no reemplazar el camino de la URL base.
 */
interface UmbralApi {

    @POST("api/auth/login")
    suspend fun login(@Body cuerpo: LoginRequest): LoginResponse

    // ---- Alumno y docente ----
    @GET("api/horario")
    suspend fun horario(): List<ClaseDto>

    @GET("api/practica/semana")
    suspend fun preguntasSemana(): List<PreguntaDto>

    @GET("api/practica/temas")
    suspend fun temas(): List<String>

    @GET("api/practica/preguntas")
    suspend fun preguntasPorTema(@Query("tema") tema: String): List<PreguntaDto>

    @POST("api/practica/respuestas")
    suspend fun registrarRespuesta(@Body cuerpo: RespuestaRequest): Response<Unit>

    @GET("api/practica/marcas")
    suspend fun marcas(): List<MarcaDto>

    @PUT("api/practica/preguntas/{id}/dificultad")
    suspend fun marcarDificultad(@Path("id") id: Int, @Body cuerpo: DificultadRequest): Response<Unit>

    @PUT("api/practica/preguntas/{id}/favorita")
    suspend fun marcarFavorita(@Path("id") id: Int, @Body cuerpo: FavoritaRequest): Response<Unit>

    @GET("api/alumno/progreso")
    suspend fun progreso(): List<ProgresoCursoDto>

    @GET("api/alumno/temas-debiles")
    suspend fun temasDebiles(): List<TemaDebilDto>

    @GET("api/alumno/comparativo")
    suspend fun comparativo(): ComparativoDto

    // ---- Docente ----
    @GET("api/docente/dudas")
    suspend fun dudas(): List<DudaDto>

    @PUT("api/docente/dudas/{id}/respuesta")
    suspend fun responderDuda(@Path("id") id: Int, @Body cuerpo: RespuestaDudaRequest): Response<Unit>

    @GET("api/docente/alumnos")
    suspend fun alumnosSeccion(): List<AlumnoSeccionDto>

    // ---- Apoderado ----
    @GET("api/apoderado/hijos")
    suspend fun hijos(): List<HijoDto>

    @GET("api/apoderado/hijos/{id}/asistencia")
    suspend fun asistenciaHijo(@Path("id") hijoId: Int): List<RegistroAsistenciaDto>

    @GET("api/apoderado/hijos/{id}/resultados")
    suspend fun resultadosHijo(@Path("id") hijoId: Int): List<ResultadoSimulacroDto>

    @GET("api/apoderado/hijos/{id}/simulacros")
    suspend fun simulacrosProgramados(@Path("id") hijoId: Int): List<SimulacroProgramadoDto>

    @GET("api/apoderado/avisos")
    suspend fun avisos(): List<AvisoDto>

    @PUT("api/apoderado/avisos/{id}/leido")
    suspend fun marcarAvisoLeido(@Path("id") id: Int): Response<Unit>
}
