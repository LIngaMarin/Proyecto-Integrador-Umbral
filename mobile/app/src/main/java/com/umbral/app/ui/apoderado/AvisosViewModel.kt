package com.umbral.app.ui.apoderado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.AvisoInasistencia
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AvisosUiState(
    val cargando: Boolean = true,
    val avisos: List<AvisoInasistencia> = emptyList(),
    val error: String? = null,
) {
    val sinLeer: Int
        get() = avisos.count { !it.leida }
}

class AvisosViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AvisosUiState())
    val estado: StateFlow<AvisosUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.avisos()
                // fecha en formato ISO (aaaa-mm-dd): el orden de texto es cronologico
                .onSuccess { lista -> _estado.update { it.copy(cargando = false, avisos = lista.sortedByDescending { a -> a.fecha }) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = mensajeDe(e)) } }
        }
    }

    fun marcarLeido(id: Int) {
        viewModelScope.launch {
            repo.marcarAvisoLeido(id)
                .onSuccess { cargar() }
                .onFailure { e -> _estado.update { it.copy(error = mensajeDe(e)) } }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AvisosViewModel(contenedor().repositorio) }
        }
    }
}
