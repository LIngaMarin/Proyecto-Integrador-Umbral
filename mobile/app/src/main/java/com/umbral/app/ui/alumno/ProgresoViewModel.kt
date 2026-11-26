package com.umbral.app.ui.alumno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.Comparativo
import com.umbral.app.domain.ProgresoCurso
import com.umbral.app.domain.TemaDebil
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgresoUiState(
    val cargando: Boolean = true,
    val cursos: List<ProgresoCurso> = emptyList(),
    val temasDebiles: List<TemaDebil> = emptyList(),
    val comparativo: Comparativo? = null,
    val error: String? = null,
)

class ProgresoViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProgresoUiState())
    val state: StateFlow<ProgresoUiState> = _state.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            coroutineScope {
                val cursosDif = async { repo.progreso() }
                val debilesDif = async { repo.temasDebiles() }
                val compDif = async { repo.comparativo() }
                val cursos = cursosDif.await()
                val debiles = debilesDif.await()
                val comp = compDif.await()

                val fallo = cursos.exceptionOrNull() ?: debiles.exceptionOrNull() ?: comp.exceptionOrNull()
                _state.value = if (fallo != null) {
                    _state.value.copy(cargando = false, error = mensajeDe(fallo))
                } else {
                    ProgresoUiState(
                        cargando = false,
                        cursos = cursos.getOrThrow(),
                        temasDebiles = debiles.getOrThrow(),
                        comparativo = comp.getOrThrow(),
                    )
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ProgresoViewModel(contenedor().repositorio) }
        }
    }
}
