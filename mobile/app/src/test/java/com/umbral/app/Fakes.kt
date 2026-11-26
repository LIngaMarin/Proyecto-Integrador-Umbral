package com.umbral.app

import com.umbral.app.data.demo.DemoRepository
import com.umbral.app.domain.Duda
import com.umbral.app.domain.ErrorUmbral
import com.umbral.app.domain.Sesion
import com.umbral.app.domain.SesionRepository
import com.umbral.app.domain.UmbralRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Repositorio que simula estar sin internet en las dudas (el resto funciona como la demo). */
class RepositorioSinConexion : UmbralRepository by DemoRepository(retardoMs = 0) {
    override suspend fun dudas(): Result<List<Duda>> = Result.failure(ErrorUmbral.SinConexion())
}

/** Sesion en memoria, en lugar de DataStore. */
class SesionEnMemoria : SesionRepository {
    private val flujo = MutableStateFlow<Sesion?>(null)
    override val sesion: StateFlow<Sesion?> = flujo
    val actual: Sesion? get() = flujo.value
    override suspend fun guardar(nueva: Sesion) { flujo.value = nueva }
    override suspend fun cerrar() { flujo.value = null }
}
