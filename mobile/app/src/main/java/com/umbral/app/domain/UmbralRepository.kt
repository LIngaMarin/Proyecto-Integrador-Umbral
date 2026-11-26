package com.umbral.app.domain

/**
 * Unica puerta de datos de la app. Las pantallas y ViewModels solo conocen esta interfaz.
 *
 * Implementaciones:
 * - DemoRepository: datos en memoria para presentar sin servidor.
 * - UmbralRepositoryRemote: API REST de Spring Boot con Retrofit (PostgreSQL detras).
 * - UmbralRepositoryOffline: envuelve a cualquiera de las dos y guarda una copia en Room.
 *
 * Todas devuelven Result: nunca lanzan la excepcion. El fallo llega como ErrorUmbral.
 * No reciben el id del usuario: el backend lo sabe por el token de la sesion.
 */
interface UmbralRepository {
    // Sesion
    suspend fun login(correo: String, clave: String): Result<Sesion>

    // Alumno y docente
    suspend fun horarioSemanal(): Result<List<Clase>>
    suspend fun preguntasSemana(): Result<List<PreguntaPractica>>
    suspend fun temas(): Result<List<String>>
    suspend fun preguntasPorTema(tema: String): Result<List<PreguntaPractica>>
    suspend fun registrarRespuesta(preguntaId: Int, opcion: Int, acerto: Boolean): Result<Unit>
    suspend fun marcasPractica(): Result<MarcasPractica>
    suspend fun marcarDificultad(preguntaId: Int, dificultad: Dificultad): Result<Unit>
    suspend fun marcarFavorita(preguntaId: Int, favorita: Boolean): Result<Unit>
    suspend fun progreso(): Result<List<ProgresoCurso>>
    suspend fun temasDebiles(): Result<List<TemaDebil>>
    suspend fun comparativo(): Result<Comparativo>

    // Docente
    suspend fun dudas(): Result<List<Duda>>
    suspend fun responderDuda(id: Int, respuesta: String): Result<Unit>
    suspend fun alumnosSeccion(): Result<List<AlumnoSeccion>>

    // Apoderado
    suspend fun hijos(): Result<List<Hijo>>
    suspend fun asistenciaHijo(hijoId: Int): Result<List<RegistroAsistencia>>
    suspend fun resultadosHijo(hijoId: Int): Result<List<ResultadoSimulacro>>
    suspend fun simulacrosProgramados(hijoId: Int): Result<List<SimulacroProgramado>>
    suspend fun avisos(): Result<List<AvisoInasistencia>>
    suspend fun marcarAvisoLeido(id: Int): Result<Unit>
}
