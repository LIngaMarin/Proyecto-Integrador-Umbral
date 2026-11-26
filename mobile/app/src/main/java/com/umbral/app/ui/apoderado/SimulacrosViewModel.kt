package com.umbral.app.ui.apoderado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.SimulacroProgramado
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimulacrosUiState(
    val cargando: Boolean = false,
    val hijoId: Int = -1,
    val cargadoDe: Int = -1,
    val agenda: List<SimulacroProgramado> = emptyList(),
    val error: String? = null,
)

class SimulacrosViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(SimulacrosUiState())
    val estado: StateFlow<SimulacrosUiState> = _estado.asStateFlow()

    private var trabajo: Job? = null

    fun cargar(hijoId: Int, forzar: Boolean = false) {
        val actual = _estado.value
        if (!forzar && (actual.cargadoDe == hijoId || (actual.cargando && actual.hijoId == hijoId))) return

        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, hijoId = hijoId, error = null) }
            repo.simulacrosProgramados(hijoId)
                .onSuccess { lista ->
                    val ordenada = lista.sortedWith(compareBy<SimulacroProgramado>({ it.fecha }, { it.hora }))
                    _estado.update { it.copy(cargando = false, cargadoDe = hijoId, agenda = ordenada) }
                }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = mensajeDe(e)) } }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SimulacrosViewModel(contenedor().repositorio) }
        }
    }
}
