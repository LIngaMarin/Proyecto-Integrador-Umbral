package com.umbral.app.ui.docente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.Duda
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DudasUiState(
    val cargando: Boolean = true,
    val dudas: List<Duda> = emptyList(),
    val soloPendientes: Boolean = true,
    val enviandoId: Int? = null,
    val error: String? = null,
) {
    val visibles: List<Duda>
        get() = if (soloPendientes) dudas.filter { !it.resuelta } else dudas
}

class DudasViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(DudasUiState())
    val estado: StateFlow<DudasUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.dudas()
                .onSuccess { lista -> _estado.update { it.copy(cargando = false, dudas = lista) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = mensajeDe(e)) } }
        }
    }

    fun elegirFiltro(soloPendientes: Boolean) {
        _estado.update { it.copy(soloPendientes = soloPendientes) }
    }

    fun responder(id: Int, texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch {
            _estado.update { it.copy(enviandoId = id, error = null) }
            repo.responderDuda(id, limpio)
                .onSuccess {
                    _estado.update { it.copy(enviandoId = null) }
                    cargar()
                }
                .onFailure { e -> _estado.update { it.copy(enviandoId = null, error = mensajeDe(e)) } }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { DudasViewModel(contenedor().repositorio) }
        }
    }
}
