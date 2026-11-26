package com.umbral.app.ui.apoderado

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.Hijo
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado compartido por las pestañas del apoderado: sus hijos y cual esta elegido. */
data class ApoderadoUiState(
    val cargando: Boolean = true,
    val hijos: List<Hijo> = emptyList(),
    /** -1 = todavia no hay hijo elegido. */
    val hijoId: Int = -1,
    val error: String? = null,
)

class ApoderadoViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(ApoderadoUiState())
    val estado: StateFlow<ApoderadoUiState> = _estado.asStateFlow()

    init {
        cargarHijos()
    }

    fun cargarHijos() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.hijos()
                .onSuccess { hijos ->
                    _estado.update { actual ->
                        val elegido = if (hijos.any { it.id == actual.hijoId }) actual.hijoId else hijos.firstOrNull()?.id ?: -1
                        actual.copy(cargando = false, hijos = hijos, hijoId = elegido)
                    }
                }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = mensajeDe(e)) } }
        }
    }

    fun elegirHijo(id: Int) {
        _estado.update { it.copy(hijoId = id) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ApoderadoViewModel(contenedor().repositorio) }
        }
    }
}
