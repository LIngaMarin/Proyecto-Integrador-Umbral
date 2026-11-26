package com.umbral.app

import android.app.Application

/** Se crea antes que cualquier pantalla y guarda el contenedor de dependencias. */
class UmbralApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
