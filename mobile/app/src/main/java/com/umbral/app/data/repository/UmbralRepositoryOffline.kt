package com.umbral.app.data.repository

import com.umbral.app.data.local.cache.CacheDao
import com.umbral.app.data.local.cache.MarcaEntity
import com.umbral.app.data.local.cache.SemanaCacheEntity
import com.umbral.app.data.local.cache.aDominio
import com.umbral.app.data.local.cache.aEntidad
import com.umbral.app.domain.Clase
import com.umbral.app.domain.Dificultad
import com.umbral.app.domain.ErrorUmbral
import com.umbral.app.domain.MarcasPractica
import com.umbral.app.domain.PreguntaPractica
import com.umbral.app.domain.UmbralRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * Modo offline: envuelve al repositorio de red ([red]) y guarda en Room lo que llega.
 * - Si la red responde, se actualiza la copia local.
 * - Si NO hay conexion, se devuelve la ultima copia guardada.
 * - La dificultad y las favoritas se guardan primero en el celular y luego se envian.
 *
 * Lo que no se sobrescribe aqui pasa directo a [red] (delegacion con "by").
 */
class UmbralRepositoryOffline(
    private val red: UmbralRepository,
    private val dao: CacheDao,
) : UmbralRepository by red {

    override suspend fun horarioSemanal(): Result<List<Clase>> = conCache(
        pedir = { red.horarioSemanal() },
        guardar = { lista -> dao.reemplazarClases(lista.map { it.aEntidad() }) },
        leer = { dao.clases().map { it.aDominio() } },
    )

    override suspend fun preguntasSemana(): Result<List<PreguntaPractica>> = conCache(
        pedir = { red.preguntasSemana() },
        guardar = { lista ->
            dao.reemplazarSemana(
                preguntas = lista.map { it.aEntidad() },
                semana = lista.mapIndexed { orden, p -> SemanaCacheEntity(orden, p.id) },
            )
        },
        leer = { dao.preguntasSemana().map { it.aDominio() } },
    )

    override suspend fun temas(): Result<List<String>> = conCache(
        pedir = { red.temas() },
        guardar = { },
        leer = { dao.temas() },
    )

    override suspend fun preguntasPorTema(tema: String): Result<List<PreguntaPractica>> = conCache(
        pedir = { red.preguntasPorTema(tema) },
        guardar = { lista -> dao.insertarPreguntas(lista.map { it.aEntidad() }) },
        leer = { dao.preguntasPorTema(tema).map { it.aDominio() } },
    )

    override suspend fun marcasPractica(): Result<MarcasPractica> {
        val remoto = red.marcasPractica()
        if (remoto.isSuccess) {
            val marcas = remoto.getOrThrow()
            seguro {
                (marcas.dificultades.keys + marcas.favoritas).forEach { id ->
                    dao.guardarMarca(MarcaEntity(id, marcas.dificultades[id]?.name, id in marcas.favoritas))
                }
            }
            return remoto
        }
        val locales = seguro { dao.marcas() } ?: emptyList()
        return Result.success(
            MarcasPractica(
                dificultades = locales.mapNotNull { m ->
                    Dificultad.entries.firstOrNull { it.name == m.dificultad }?.let { m.preguntaId to it }
                }.toMap(),
                favoritas = locales.filter { it.favorita }.map { it.preguntaId }.toSet(),
            )
        )
    }

    override suspend fun marcarDificultad(preguntaId: Int, dificultad: Dificultad): Result<Unit> {
        seguro {
            val actual = dao.marca(preguntaId)
            dao.guardarMarca(MarcaEntity(preguntaId, dificultad.name, actual?.favorita ?: false))
        }
        red.marcarDificultad(preguntaId, dificultad) // si falla, ya quedo guardada en el celular
        return Result.success(Unit)
    }

    override suspend fun marcarFavorita(preguntaId: Int, favorita: Boolean): Result<Unit> {
        seguro {
            val actual = dao.marca(preguntaId)
            dao.guardarMarca(MarcaEntity(preguntaId, actual?.dificultad, favorita))
        }
        red.marcarFavorita(preguntaId, favorita)
        return Result.success(Unit)
    }

    /** Pide a la red; si funciona guarda copia; si no hay conexion, lee la copia local. */
    private suspend fun <T> conCache(
        pedir: suspend () -> Result<T>,
        guardar: suspend (T) -> Unit,
        leer: suspend () -> T,
    ): Result<T> {
        val resultado = pedir()
        if (resultado.isSuccess) {
            val datos = resultado.getOrThrow()
            seguro { guardar(datos) }
            return resultado
        }
        if (resultado.exceptionOrNull() !is ErrorUmbral.SinConexion) return resultado
        val local = seguro { leer() }
        val vacio = local == null || (local is Collection<*> && local.isEmpty())
        return if (vacio) resultado else Result.success(local as T)
    }

    /** Si Room falla, la app sigue: la cache es una ayuda, no la fuente principal. */
    private suspend fun <T> seguro(bloque: suspend () -> T): T? =
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
