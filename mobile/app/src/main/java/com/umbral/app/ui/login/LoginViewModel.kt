package com.umbral.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.umbral.app.domain.CuentaDemo
import com.umbral.app.domain.SesionRepository
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.contenedor
import com.umbral.app.ui.common.mensajeDe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val correo: String = "",
    val clave: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
) {
    val puedeIngresar: Boolean
        get() = correo.isNotBlank() && clave.isNotBlank() && !cargando
}

/**
 * Login contra el repositorio (Spring Boot o demo). Si es correcto guarda la sesion;
 * la navegacion (UmbralApp) detecta la sesion nueva y lleva al inicio del rol.
 */
class LoginViewModel(
    private val repo: UmbralRepository,
    private val sesion: SesionRepository,
    val cuentasDemo: List<CuentaDemo>,
) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun cambiarCorreo(texto: String) = _estado.update { it.copy(correo = texto, error = null) }

    fun cambiarClave(texto: String) = _estado.update { it.copy(clave = texto, error = null) }

    fun ingresar() {
        val actual = _estado.value
        if (!actual.puedeIngresar) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val resultado = repo.login(actual.correo.trim(), actual.clave)
            val nueva = resultado.getOrNull()
            if (nueva != null) {
                sesion.guardar(nueva)
                _estado.update { it.copy(cargando = false, clave = "") }
            } else {
                val fallo = resultado.exceptionOrNull() ?: IllegalStateException()
                _estado.update { it.copy(cargando = false, error = mensajeDe(fallo)) }
            }
        }
    }

    /** Modo demo: llena el formulario con la cuenta elegida e ingresa. */
    fun entrarComo(cuenta: CuentaDemo) {
        _estado.update { it.copy(correo = cuenta.correo, clave = cuenta.clave, error = null) }
        ingresar()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = contenedor()
                LoginViewModel(c.repositorio, c.sesion, c.cuentasDemo)
            }
        }
    }
}
