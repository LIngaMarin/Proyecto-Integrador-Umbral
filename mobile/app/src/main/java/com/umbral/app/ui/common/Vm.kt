package com.umbral.app.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.umbral.app.AppContainer
import com.umbral.app.UmbralApplication

/**
 * Para usar dentro de viewModelFactory { initializer { ... } }:
 * da acceso al AppContainer de la aplicacion.
 *
 *   val Factory = viewModelFactory { initializer { MiViewModel(contenedor().repositorio) } }
 */
fun CreationExtras.contenedor(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UmbralApplication).container
