package com.umbral.app.data.remote

import com.umbral.app.domain.AlumnoSeccion
import com.umbral.app.domain.AvisoInasistencia
import com.umbral.app.domain.Clase
import com.umbral.app.domain.Comparativo
import com.umbral.app.domain.Dificultad
import com.umbral.app.domain.Duda
import com.umbral.app.domain.ErrorUmbral
import com.umbral.app.domain.EstadoAsistencia
import com.umbral.app.domain.Hijo
import com.umbral.app.domain.MarcasPractica
import com.umbral.app.domain.PreguntaPractica
import com.umbral.app.domain.ProgresoCurso
import com.umbral.app.domain.RegistroAsistencia
import com.umbral.app.domain.ResultadoSimulacro
import com.umbral.app.domain.Rol
import com.umbral.app.domain.Sesion
import com.umbral.app.domain.SimulacroProgramado
import com.umbral.app.domain.TemaDebil
import com.umbral.app.domain.Usuario
import kotlinx.serialization.Serializable

// DTO = forma exacta del JSON que envia/recibe Spring Boot.
// Los nombres de las propiedades deben ser iguales a los del JSON del backend.
// Los enums viajan como texto ("ALUMNO", "FACIL", "PRESENTE").

@Serializable data class LoginRequest(val correo: String, val clave: String)
@Serializable data class UsuarioDto(val id: Int, val nombre: String, val rol: String, val detalle: String = "")
@Serializable data class LoginResponse(val token: String, val usuario: UsuarioDto)

@Serializable data class ClaseDto(
    val id: Int, val curso: String, val seccion: String, val aula: String, val sede: String,
    val dia: Int, val horaInicio: String, val horaFin: String, val docente: String,
)

@Serializable data class PreguntaDto(
    val id: Int, val tema: String, val enunciado: String, val opciones: List<String>,
    val correcta: Int, val solucionario: List<String> = emptyList(), val dificultad: String = "MEDIA",
)

@Serializable data class RespuestaRequest(val preguntaId: Int, val opcion: Int, val acerto: Boolean)
@Serializable data class MarcaDto(val preguntaId: Int, val dificultad: String? = null, val favorita: Boolean = false)
@Serializable data class DificultadRequest(val dificultad: String)
@Serializable data class FavoritaRequest(val favorita: Boolean)

@Serializable data class ProgresoCursoDto(val curso: String, val completadas: Int, val totales: Int)
@Serializable data class TemaDebilDto(val tema: String, val aciertoPct: Int)
@Serializable data class ComparativoDto(val miPromedio: Int, val promedioSeccion: Int)

@Serializable data class DudaDto(
    val id: Int, val alumno: String, val tema: String, val texto: String,
    val resuelta: Boolean, val respuesta: String? = null,
)
@Serializable data class RespuestaDudaRequest(val respuesta: String)
@Serializable data class AlumnoSeccionDto(val id: Int, val nombre: String, val seccion: String, val asistenciaPct: Int)

@Serializable data class HijoDto(val id: Int, val nombre: String, val seccion: String)
@Serializable data class RegistroAsistenciaDto(val fecha: String, val estado: String)
@Serializable data class ResultadoSimulacroDto(val nombre: String, val fecha: String, val puntaje: Int)
@Serializable data class SimulacroProgramadoDto(val nombre: String, val fecha: String, val hora: String, val aula: String)
@Serializable data class AvisoDto(val id: Int, val mensaje: String, val fecha: String, val leida: Boolean)

// ---- Conversion DTO -> modelo de la app ----

/** Lee un enum por su nombre; si el backend manda otro valor, es una respuesta inesperada. */
private inline fun <reified E : Enum<E>> enumDe(valor: String): E =
    enumValues<E>().firstOrNull { it.name.equals(valor, ignoreCase = true) }
        ?: throw ErrorUmbral.RespuestaInesperada()

private fun dificultadDe(valor: String?): Dificultad? =
    Dificultad.entries.firstOrNull { it.name.equals(valor, ignoreCase = true) }

fun LoginResponse.aDominio() = Sesion(usuario.aDominio(), token)
fun UsuarioDto.aDominio() = Usuario(id, nombre, enumDe<Rol>(rol), detalle)
fun ClaseDto.aDominio() = Clase(id, curso, seccion, aula, sede, dia, horaInicio, horaFin, docente)
fun PreguntaDto.aDominio() = PreguntaPractica(
    id, tema, enunciado, opciones, correcta, solucionario, dificultadDe(dificultad) ?: Dificultad.MEDIA,
)
fun List<MarcaDto>.aMarcas() = MarcasPractica(
    dificultades = mapNotNull { m -> dificultadDe(m.dificultad)?.let { m.preguntaId to it } }.toMap(),
    favoritas = filter { it.favorita }.map { it.preguntaId }.toSet(),
)
fun ProgresoCursoDto.aDominio() = ProgresoCurso(curso, completadas, totales)
fun TemaDebilDto.aDominio() = TemaDebil(tema, aciertoPct)
fun ComparativoDto.aDominio() = Comparativo(miPromedio, promedioSeccion)
fun DudaDto.aDominio() = Duda(id, alumno, tema, texto, resuelta, respuesta)
fun AlumnoSeccionDto.aDominio() = AlumnoSeccion(id, nombre, seccion, asistenciaPct)
fun HijoDto.aDominio() = Hijo(id, nombre, seccion)
fun RegistroAsistenciaDto.aDominio() = RegistroAsistencia(fecha, enumDe<EstadoAsistencia>(estado))
fun ResultadoSimulacroDto.aDominio() = ResultadoSimulacro(nombre, fecha, puntaje)
fun SimulacroProgramadoDto.aDominio() = SimulacroProgramado(nombre, fecha, hora, aula)
fun AvisoDto.aDominio() = AvisoInasistencia(id, mensaje, fecha, leida)
