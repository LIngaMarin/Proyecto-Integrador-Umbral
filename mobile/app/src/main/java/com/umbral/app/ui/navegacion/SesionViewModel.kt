package com.umbral.app.ui.navegacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.SesionRepository
import com.umbral.app.domain.Usuario
import com.umbral.app.ui.common.contenedor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EstadoSesion {
    /** Todavia se esta leyendo la sesion guardada (dura un instante al abrir la app). */
    data object Leyendo : EstadoSesion
    data object SinSesion : EstadoSesion
    data class ConSesion(val usuario: Usuario) : EstadoSesion
}

/**
 * Fuente unica de verdad de la sesion. La navegacion reacciona a este estado:
 * login correcto -> va al inicio del rol; cerrar sesion (o token vencido) -> vuelve al login.
 */
class SesionViewModel(
    sesion: SesionRepository,
    private val cerrar: suspend () -> Unit,
) : ViewModel() {

    val estado: StateFlow<EstadoSesion> = sesion.sesion
        .map { s -> if (s == null) EstadoSesion.SinSesion else EstadoSesion.ConSesion(s.usuario) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoSesion.Leyendo)

    fun cerrarSesion() {
        viewModelScope.launch { cerrar() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                SesionViewModel(c.sesion, cerrar = { c.cerrarSesion() })
            }
        }
    }
}
