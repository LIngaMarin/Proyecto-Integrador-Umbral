package com.umbral.app.ui.docente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.Clase
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.domain.momentoActual
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Clases de un dia del horario (dia: 1 = lunes ... 7 = domingo). */
data class DiaClases(val dia: Int, val clases: List<Clase>)

data class HoyUiState(
    val cargando: Boolean = true,
    val clasesHoy: List<Clase> = emptyList(),
    val semana: List<DiaClases> = emptyList(),
    val error: String? = null,
)

class HoyViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(HoyUiState())
    val estado: StateFlow<HoyUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.horarioSemanal()
                .onSuccess { clases ->
                    val hoy = momentoActual().dia
                    val semana = clases
                        .groupBy { it.dia }
                        .toList()
                        .sortedBy { it.first }
                        .map { (dia, lista) -> DiaClases(dia, lista.sortedBy { it.horaInicio }) }
                    _estado.value = HoyUiState(
                        cargando = false,
                        clasesHoy = semana.firstOrNull { it.dia == hoy }?.clases ?: emptyList(),
                        semana = semana,
                    )
                }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = mensajeDe(e)) } }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HoyViewModel(contenedor().repositorio) }
        }
    }
}
