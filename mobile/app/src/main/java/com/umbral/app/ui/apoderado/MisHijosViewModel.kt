package com.umbral.app.ui.apoderado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbral.app.domain.EstadoAsistencia
import com.umbral.app.domain.RegistroAsistencia
import com.umbral.app.domain.ResultadoSimulacro
import com.umbral.app.domain.UmbralRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val asistencia: List<RegistroAsistencia> = emptyList(),
    val resultados: List<ResultadoSimulacro> = emptyList(),
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

    /** Compara los dos últimos resultados (se asume orden cronológico: el último va al final). */
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

    fun cargar(hijoId: Int) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, hijoId = hijoId)
            val asistencia = repo.asistenciaHijo(hijoId)
            val resultados = repo.resultadosHijo(hijoId)
            // Si mientras tanto se eligió otro hijo, este resultado ya no corresponde.
            if (_estado.value.hijoId != hijoId) return@launch
            _estado.value = _estado.value.copy(
                cargando = false,
                asistencia = asistencia,
                resultados = resultados,
            )
        }
    }
}
