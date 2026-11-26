package com.umbral.app

import android.content.Context
import com.umbral.app.data.demo.CUENTAS_DEMO
import com.umbral.app.data.demo.DemoRepository
import com.umbral.app.data.local.SesionDataStore
import com.umbral.app.data.local.UmbralDatabase
import com.umbral.app.data.remote.ApiUmbral
import com.umbral.app.data.remote.UmbralRepositoryRemote
import com.umbral.app.data.repository.UmbralRepositoryOffline
import com.umbral.app.domain.CuentaDemo
import com.umbral.app.domain.UmbralRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Inyeccion de dependencias manual: aqui se crean UNA sola vez Retrofit, Room,
 * la sesion y el repositorio. Los ViewModels los piden por su Factory;
 * ninguna pantalla crea dependencias.
 *
 * Para pasar de la demo al servidor real: en app/build.gradle.kts poner USAR_DEMO = false.
 */
class AppContainer(private val context: Context) {

    val sesion: SesionDataStore = SesionDataStore(context)

    private val baseDatos: UmbralDatabase by lazy { UmbralDatabase.crear(context) }

    /** Cuentas para "entrar rapido" en el login. Vacia cuando se usa el servidor real. */
    val cuentasDemo: List<CuentaDemo> = if (BuildConfig.USAR_DEMO) CUENTAS_DEMO else emptyList()

    val repositorio: UmbralRepository by lazy {
        val origen: UmbralRepository = if (BuildConfig.USAR_DEMO) {
            DemoRepository()
        } else {
            UmbralRepositoryRemote(
                api = ApiUmbral.crear(
                    urlBase = BuildConfig.API_URL,
                    tokenActual = { sesion.tokenActual },
                    depurar = BuildConfig.DEBUG,
                ),
                alExpirarSesion = { cerrarSesion() },
            )
        }
        UmbralRepositoryOffline(origen, baseDatos.cacheDao())
    }

    /** Cierra la sesion y borra la cache local (para que otro usuario no vea esos datos). */
    suspend fun cerrarSesion() {
        withContext(Dispatchers.IO) { baseDatos.clearAllTables() } // es bloqueante: fuera del hilo principal
        sesion.cerrar()
    }
}
