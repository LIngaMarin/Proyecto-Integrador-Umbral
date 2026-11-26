package com.umbral.app.ui.alumno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.Clase
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.domain.nombreDia
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiaClases(val nombre: String, val clases: List<Clase>)

data class HorarioUiState(
    val cargando: Boolean = true,
    val dias: List<DiaClases> = emptyList(),
    val error: String? = null,
)

class HorarioViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _state = MutableStateFlow(HorarioUiState())
    val state: StateFlow<HorarioUiState> = _state.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            repo.horarioSemanal()
                .onSuccess { clases ->
                    val dias = (1..7).mapNotNull { dia ->
                        val delDia = clases.filter { it.dia == dia }.sortedBy { it.horaInicio }
                        if (delDia.isEmpty()) null else DiaClases(nombreDia(dia), delDia)
                    }
                    _state.value = HorarioUiState(cargando = false, dias = dias)
                }
                .onFailure { e -> _state.update { it.copy(cargando = false, error = mensajeDe(e)) } }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HorarioViewModel(contenedor().repositorio) }
        }
    }
}
