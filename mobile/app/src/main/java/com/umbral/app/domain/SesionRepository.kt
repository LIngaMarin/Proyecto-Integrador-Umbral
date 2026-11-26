package com.umbral.app.domain

import kotlinx.coroutines.flow.Flow

/** Donde se guarda la sesion en el celular. La implementa SesionDataStore (DataStore). */
interface SesionRepository {
    /** null = no hay sesion. Emite de nuevo cada vez que cambia. */
    val sesion: Flow<Sesion?>
    suspend fun guardar(nueva: Sesion)
    suspend fun cerrar()
}
