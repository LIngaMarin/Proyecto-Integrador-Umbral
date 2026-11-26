package com.umbral.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.umbral.app.domain.Rol
import com.umbral.app.domain.Sesion
import com.umbral.app.domain.SesionRepository
import com.umbral.app.domain.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.io.IOException

private val Context.almacenSesion: DataStore<Preferences> by preferencesDataStore(name = "sesion")

/**
 * Guarda la sesion (token y datos del usuario) con DataStore.
 * Asi la sesion sobrevive a rotar la pantalla y a cerrar la app.
 */
class SesionDataStore(context: Context) : SesionRepository {

    private val almacen = context.applicationContext.almacenSesion

    /** Copia en memoria del token para que el interceptor de OkHttp lo lea sin esperar. */
    @Volatile
    var tokenActual: String? = null
        private set

    override val sesion: Flow<Sesion?> = almacen.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> leer(prefs) }
        .onEach { tokenActual = it?.token }

    override suspend fun guardar(nueva: Sesion) {
        tokenActual = nueva.token
        almacen.edit { prefs ->
            prefs[TOKEN] = nueva.token
            prefs[ID] = nueva.usuario.id
            prefs[NOMBRE] = nueva.usuario.nombre
            prefs[ROL] = nueva.usuario.rol.name
            prefs[DETALLE] = nueva.usuario.detalle
        }
    }

    override suspend fun cerrar() {
        tokenActual = null
        almacen.edit { it.clear() }
    }

    private fun leer(prefs: Preferences): Sesion? {
        val token = prefs[TOKEN] ?: return null
        val id = prefs[ID] ?: return null
        val rol = Rol.entries.firstOrNull { it.name == prefs[ROL] } ?: return null
        val usuario = Usuario(id, prefs[NOMBRE].orEmpty(), rol, prefs[DETALLE].orEmpty())
        return Sesion(usuario, token)
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val ID = intPreferencesKey("usuario_id")
        val NOMBRE = stringPreferencesKey("usuario_nombre")
        val ROL = stringPreferencesKey("usuario_rol")
        val DETALLE = stringPreferencesKey("usuario_detalle")
    }
}
