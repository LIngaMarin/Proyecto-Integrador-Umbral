package com.umbral.app.domain

enum class Rol(val titulo: String) {
    ALUMNO("Alumno"),
    DOCENTE("Docente"),
    APODERADO("Apoderado"),
}

data class Usuario(val id: Int, val nombre: String, val rol: Rol, val detalle: String)

/** dia: 1 = lunes ... 6 = sábado. Horas en formato "08:00". */
data class Clase(
    val id: Int,
    val curso: String,
    val seccion: String,
    val aula: String,
    val sede: String,
    val dia: Int,
    val horaInicio: String,
    val horaFin: String,
    val docente: String,
)

enum class Dificultad(val texto: String) { FACIL("Fácil"), MEDIA("Media"), DIFICIL("Difícil") }

data class PreguntaPractica(
    val id: Int,
    val tema: String,
    val enunciado: String,
    val opciones: List<String>,
    val correcta: Int, // índice dentro de opciones
    val solucionario: List<String>, // pasos
    val dificultad: Dificultad,
)

data class ProgresoCurso(val curso: String, val completadas: Int, val totales: Int)

/** aciertoPct de 0 a 100. */
data class TemaDebil(val tema: String, val aciertoPct: Int)

/** Promedios sobre 100. El de la sección es anónimo: no expone a otros alumnos. */
data class Comparativo(val miPromedio: Int, val promedioSeccion: Int)

data class Duda(
    val id: Int,
    val alumno: String,
    val tema: String,
    val texto: String,
    val resuelta: Boolean,
    val respuesta: String?,
)

data class AlumnoSeccion(val id: Int, val nombre: String, val seccion: String, val asistenciaPct: Int)

data class Hijo(val id: Int, val nombre: String, val seccion: String)

enum class EstadoAsistencia(val texto: String) {
    PRESENTE("Asistió"),
    TARDANZA("Tardanza"),
    FALTA("Faltó"),
}

data class RegistroAsistencia(val fecha: String, val estado: EstadoAsistencia)

data class ResultadoSimulacro(val nombre: String, val fecha: String, val puntaje: Int)

data class SimulacroProgramado(val nombre: String, val fecha: String, val hora: String, val aula: String)

data class AvisoInasistencia(val id: Int, val mensaje: String, val fecha: String, val leida: Boolean)
