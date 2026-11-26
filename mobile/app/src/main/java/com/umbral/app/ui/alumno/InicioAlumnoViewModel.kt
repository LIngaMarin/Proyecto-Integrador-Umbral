package com.umbral.app.ui.alumno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.Clase
import com.umbral.app.domain.TemaDebil
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.domain.momentoActual
import com.umbral.app.domain.proximaClase
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InicioAlumnoUiState(
    val cargando: Boolean = true,
    val avancePct: Int = 0,
    val proximaClase: Clase? = null,
    val temasDebiles: List<TemaDebil> = emptyList(),
    val error: String? = null,
)

class InicioAlumnoViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _state = MutableStateFlow(InicioAlumnoUiState())
    val state: StateFlow<InicioAlumnoUiState> = _state.asStateFlow()

    init {
        cargar()
    }

    /** Pide progreso, horario y temas debiles A LA VEZ (async) y junta los resultados. */
    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            coroutineScope {
                val progresoDif = async { repo.progreso() }
                val horarioDif = async { repo.horarioSemanal() }
                val debilesDif = async { repo.temasDebiles() }
                val progreso = progresoDif.await()
                val horario = horarioDif.await()
                val debiles = debilesDif.await()

                val fallo = progreso.exceptionOrNull() ?: horario.exceptionOrNull() ?: debiles.exceptionOrNull()
                if (fallo != null) {
                    _state.update { it.copy(cargando = false, error = mensajeDe(fallo)) }
                } else {
                    val cursos = progreso.getOrThrow()
                    val totales = cursos.sumOf { it.totales }
                    val completadas = cursos.sumOf { it.completadas }
                    _state.value = InicioAlumnoUiState(
                        cargando = false,
                        avancePct = if (totales == 0) 0 else completadas * 100 / totales,
                        proximaClase = proximaClase(horario.getOrThrow(), momentoActual()),
                        temasDebiles = debiles.getOrThrow().sortedBy { it.aciertoPct }.take(2),
                    )
                }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { InicioAlumnoViewModel(contenedor().repositorio) }
        }
    }
}
