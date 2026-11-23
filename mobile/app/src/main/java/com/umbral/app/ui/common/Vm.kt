package com.umbral.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Fábrica mínima para crear ViewModels con dependencias: viewModel(factory = vmFactory { MiVm(repo) }). */
inline fun <reified T : ViewModel> vmFactory(crossinline construir: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V = construir() as V
    }
