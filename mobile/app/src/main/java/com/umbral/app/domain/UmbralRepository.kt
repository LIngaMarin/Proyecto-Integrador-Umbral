package com.umbral.app.domain

/**
 * Única puerta de datos de la app. Hoy la implementa [com.umbral.app.data.demo.DemoRepository]
 * (datos locales de demostración). Para conectar el backend se crea otra implementación
 * (por ejemplo con Retrofit hacia /api/...) y se cambia en MainActivity; las pantallas no cambian.
 */
interface UmbralRepository {
    suspend fun usuariosDemo(): List<Usuario>

    // Alumno y docente
    suspend fun horarioSemanal(): List<Clase>
    suspend fun preguntasSemana(): List<PreguntaPractica>
    suspend fun temas(): List<String>
    suspend fun preguntasPorTema(tema: String): List<PreguntaPractica>
    suspend fun progreso(): List<ProgresoCurso>
    suspend fun temasDebiles(): List<TemaDebil>
    suspend fun comparativo(): Comparativo

    // Docente
    suspend fun dudas(): List<Duda>
    suspend fun responderDuda(id: Int, respuesta: String)
    suspend fun alumnosSeccion(): List<AlumnoSeccion>

    // Apoderado
    suspend fun hijos(): List<Hijo>
    suspend fun asistenciaHijo(hijoId: Int): List<RegistroAsistencia>
    suspend fun resultadosHijo(hijoId: Int): List<ResultadoSimulacro>
    suspend fun simulacrosProgramados(hijoId: Int): List<SimulacroProgramado>
    suspend fun avisos(): List<AvisoInasistencia>
    suspend fun marcarAvisoLeido(id: Int)
}
