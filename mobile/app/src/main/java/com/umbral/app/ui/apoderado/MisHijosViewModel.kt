package com.umbral.app.ui.apoderado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.EstadoAsistencia
import com.umbral.app.domain.RegistroAsistencia
import com.umbral.app.domain.ResultadoSimulacro
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class Tendencia(val texto: String) {
    SUBE("Subió"),
    BAJA("Bajó"),
    ESTABLE("Se mantiene"),
    SIN_DATOS("Sin datos suficientes"),
}

data class MisHijosUiState(
    val cargando: Boolean = false,
    val hijoId: Int = -1,
    /** Hijo cuyos datos ya estan cargados (-1 = ninguno). */
    val cargadoDe: Int = -1,
    val asistencia: List<RegistroAsistencia> = emptyList(),
    val resultados: List<ResultadoSimulacro> = emptyList(),
    val error: String? = null,
) {
    val presentes: Int
        get() = asistencia.count { it.estado == EstadoAsistencia.PRESENTE }

    val tardanzas: Int
        get() = asistencia.count { it.estado == EstadoAsistencia.TARDANZA }

    val faltas: Int
        get() = asistencia.count { it.estado == EstadoAsistencia.FALTA }

    /** Asistencia = presentes + tardanzas, sobre el total de registros. */
    val porcentaje: Int
        get() = if (asistencia.isEmpty()) 0 else ((presentes + tardanzas) * 100.0 / asistencia.size).roundToInt()

    /** Compara los dos ultimos resultados (se asume orden cronologico: el ultimo va al final). */
    val tendencia: Tendencia
        get() {
            if (resultados.size < 2) return Tendencia.SIN_DATOS
            val anterior = resultados[resultados.size - 2].puntaje
            val ultimo = resultados[resultados.size - 1].puntaje
            return when {
                ultimo > anterior -> Tendencia.SUBE
                ultimo < anterior -> Tendencia.BAJA
                else -> Tendencia.ESTABLE
            }
        }
}

class MisHijosViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(MisHijosUiState())
    val estado: StateFlow<MisHijosUiState> = _estado.asStateFlow()

    private var trabajo: Job? = null

    /**
     * Carga asistencia y resultados del hijo A LA VEZ.
     * Si ya estan cargados no vuelve a pedirlos (salvo [forzar] = true, el boton Reintentar).
     */
    fun cargar(hijoId: Int, forzar: Boolean = false) {
        val actual = _estado.value
        if (!forzar && (actual.cargadoDe == hijoId || (actual.cargando && actual.hijoId == hijoId))) return

        trabajo?.cancel() // si se eligio otro hijo, la carga anterior ya no sirve
        trabajo = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, hijoId = hijoId, error = null) }
            coroutineScope {
                val asistenciaDif = async { repo.asistenciaHijo(hijoId) }
                val resultadosDif = async { repo.resultadosHijo(hijoId) }
                val asistencia = asistenciaDif.await()
                val resultados = resultadosDif.await()

                val fallo = asistencia.exceptionOrNull() ?: resultados.exceptionOrNull()
                _estado.update {
                    if (fallo != null) {
                        it.copy(cargando = false, error = mensajeDe(fallo))
                    } else {
                        it.copy(
                            cargando = false,
                            cargadoDe = hijoId,
                            asistencia = asistencia.getOrThrow(),
                            resultados = resultados.getOrThrow(),
                        )
                    }
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { MisHijosViewModel(contenedor().repositorio) }
        }
    }
}
