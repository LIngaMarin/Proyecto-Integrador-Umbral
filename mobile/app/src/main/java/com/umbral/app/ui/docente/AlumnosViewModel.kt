package com.umbral.app.ui.docente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.AlumnoSeccion
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlumnosUiState(
    val cargando: Boolean = true,
    val todos: List<AlumnoSeccion> = emptyList(),
    val busqueda: String = "",
    val error: String? = null,
) {
    /** La busqueda filtra lo ya descargado: no vuelve a llamar a la API. */
    val visibles: List<AlumnoSeccion>
        get() = todos.filter { it.nombre.contains(busqueda.trim(), ignoreCase = true) }
}

class AlumnosViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AlumnosUiState())
    val estado: StateFlow<AlumnosUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            repo.alumnosSeccion()
                .onSuccess { lista -> _estado.update { it.copy(cargando = false, todos = lista) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = mensajeDe(e)) } }
        }
    }

    fun cambiarBusqueda(texto: String) {
        _estado.update { it.copy(busqueda = texto) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { AlumnosViewModel(contenedor().repositorio) }
        }
    }
}
