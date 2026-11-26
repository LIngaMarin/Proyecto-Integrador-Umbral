package com.umbral.app.data.remote

import com.umbral.app.domain.AlumnoSeccion
import com.umbral.app.domain.AvisoInasistencia
import com.umbral.app.domain.Clase
import com.umbral.app.domain.Comparativo
import com.umbral.app.domain.Dificultad
import com.umbral.app.domain.Duda
import com.umbral.app.domain.ErrorUmbral
import com.umbral.app.domain.Hijo
import com.umbral.app.domain.MarcasPractica
import com.umbral.app.domain.PreguntaPractica
import com.umbral.app.domain.ProgresoCurso
import com.umbral.app.domain.RegistroAsistencia
import com.umbral.app.domain.ResultadoSimulacro
import com.umbral.app.domain.Sesion
import com.umbral.app.domain.SimulacroProgramado
import com.umbral.app.domain.TemaDebil
import com.umbral.app.domain.UmbralRepository
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * UmbralRepository contra la API REST de Spring Boot.
 * Ninguna excepcion de red sale de aqui: todo se convierte en Result.failure(ErrorUmbral).
 */
class UmbralRepositoryRemote(
    private val api: UmbralApi,
    /** Se llama si el backend responde 401: el token vencio y hay que volver al login. */
    private val alExpirarSesion: suspend () -> Unit,
) : UmbralRepository {

    override suspend fun login(correo: String, clave: String): Result<Sesion> =
        llamar(esLogin = true) { api.login(LoginRequest(correo, clave)).aDominio() }

    override suspend fun horarioSemanal(): Result<List<Clase>> =
        llamar { api.horario().map { it.aDominio() } }

    override suspend fun preguntasSemana(): Result<List<PreguntaPractica>> =
        llamar { api.preguntasSemana().map { it.aDominio() } }

    override suspend fun temas(): Result<List<String>> = llamar { api.temas() }

    override suspend fun preguntasPorTema(tema: String): Result<List<PreguntaPractica>> =
        llamar { api.preguntasPorTema(tema).map { it.aDominio() } }

    override suspend fun registrarRespuesta(preguntaId: Int, opcion: Int, acerto: Boolean): Result<Unit> =
        sinCuerpo { api.registrarRespuesta(RespuestaRequest(preguntaId, opcion, acerto)) }

    override suspend fun marcasPractica(): Result<MarcasPractica> = llamar { api.marcas().aMarcas() }

    override suspend fun marcarDificultad(preguntaId: Int, dificultad: Dificultad): Result<Unit> =
        sinCuerpo { api.marcarDificultad(preguntaId, DificultadRequest(dificultad.name)) }

    override suspend fun marcarFavorita(preguntaId: Int, favorita: Boolean): Result<Unit> =
        sinCuerpo { api.marcarFavorita(preguntaId, FavoritaRequest(favorita)) }

    override suspend fun progreso(): Result<List<ProgresoCurso>> = llamar { api.progreso().map { it.aDominio() } }

    override suspend fun temasDebiles(): Result<List<TemaDebil>> = llamar { api.temasDebiles().map { it.aDominio() } }

    override suspend fun comparativo(): Result<Comparativo> = llamar { api.comparativo().aDominio() }

    override suspend fun dudas(): Result<List<Duda>> = llamar { api.dudas().map { it.aDominio() } }

    override suspend fun responderDuda(id: Int, respuesta: String): Result<Unit> =
        sinCuerpo { api.responderDuda(id, RespuestaDudaRequest(respuesta)) }

    override suspend fun alumnosSeccion(): Result<List<AlumnoSeccion>> =
        llamar { api.alumnosSeccion().map { it.aDominio() } }

    override suspend fun hijos(): Result<List<Hijo>> = llamar { api.hijos().map { it.aDominio() } }

    override suspend fun asistenciaHijo(hijoId: Int): Result<List<RegistroAsistencia>> =
        llamar { api.asistenciaHijo(hijoId).map { it.aDominio() } }

    override suspend fun resultadosHijo(hijoId: Int): Result<List<ResultadoSimulacro>> =
        llamar { api.resultadosHijo(hijoId).map { it.aDominio() } }

    override suspend fun simulacrosProgramados(hijoId: Int): Result<List<SimulacroProgramado>> =
        llamar { api.simulacrosProgramados(hijoId).map { it.aDominio() } }

    override suspend fun avisos(): Result<List<AvisoInasistencia>> = llamar { api.avisos().map { it.aDominio() } }

    override suspend fun marcarAvisoLeido(id: Int): Result<Unit> = sinCuerpo { api.marcarAvisoLeido(id) }

    /** Para PUT/POST sin cuerpo de respuesta: si el codigo no es 2xx se trata como HttpException. */
    private suspend fun sinCuerpo(bloque: suspend () -> Response<Unit>): Result<Unit> = llamar {
        val respuesta = bloque()
        if (!respuesta.isSuccessful) throw HttpException(respuesta)
    }

    /** Un solo try/catch para toda la API. Traduce cada falla a un ErrorUmbral. */
    private suspend fun <T> llamar(esLogin: Boolean = false, bloque: suspend () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e // nunca se traga la cancelacion de la corrutina
        } catch (e: ErrorUmbral) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(ErrorUmbral.SinConexion())
        } catch (e: HttpException) {
            when {
                esLogin && (e.code() == 400 || e.code() == 401) -> Result.failure(ErrorUmbral.CredencialesInvalidas())
                e.code() == 401 -> {
                    alExpirarSesion()
                    Result.failure(ErrorUmbral.SesionExpirada())
                }
                else -> Result.failure(ErrorUmbral.Servidor(e.code()))
            }
        } catch (e: SerializationException) {
            Result.failure(ErrorUmbral.RespuestaInesperada())
        } catch (e: Exception) {
            Result.failure(e)
        }
}
