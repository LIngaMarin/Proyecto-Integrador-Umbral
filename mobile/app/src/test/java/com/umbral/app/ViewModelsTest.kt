package com.umbral.app

import com.umbral.app.data.demo.CLAVE_DEMO
import com.umbral.app.data.demo.CUENTAS_DEMO
import com.umbral.app.data.demo.DemoRepository
import com.umbral.app.domain.ResultadoSimulacro
import com.umbral.app.domain.Rol
import com.umbral.app.ui.apoderado.MisHijosUiState
import com.umbral.app.ui.apoderado.Tendencia
import com.umbral.app.ui.docente.AlumnosViewModel
import com.umbral.app.ui.docente.DudasViewModel
import com.umbral.app.ui.login.LoginViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    @get:Rule
    val main = MainDispatcherRule()

    // ---- Dudas (docente) ----
    @Test
    fun dudasAlIniciarMuestraSoloPendientes() = runTest {
        val vm = DudasViewModel(DemoRepository(retardoMs = 0))
        advanceUntilIdle()
        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertTrue(estado.visibles.isNotEmpty())
        assertTrue(estado.visibles.none { it.resuelta })
    }

    @Test
    fun responderUnaDudaLaSacaDePendientes() = runTest {
        val vm = DudasViewModel(DemoRepository(retardoMs = 0))
        advanceUntilIdle()
        vm.responder(1, "Porque se aplica la operación inversa en ambos lados.")
        advanceUntilIdle()
        assertTrue(vm.estado.value.visibles.none { it.id == 1 })
    }

    @Test
    fun siNoHayConexionLasDudasMuestranError() = runTest {
        val vm = DudasViewModel(RepositorioSinConexion())
        advanceUntilIdle()
        assertFalse(vm.estado.value.cargando)
        assertNotNull(vm.estado.value.error)
    }

    // ---- Alumnos (docente) ----
    @Test
    fun laBusquedaFiltraPorNombreSinImportarMayusculas() = runTest {
        val vm = AlumnosViewModel(DemoRepository(retardoMs = 0))
        advanceUntilIdle()
        vm.cambiarBusqueda("  camila ")
        val visibles = vm.estado.value.visibles
        assertEquals(1, visibles.size)
        assertEquals("Camila Rojas", visibles.first().nombre)
    }

    // ---- Login ----
    @Test
    fun loginCorrectoGuardaLaSesion() = runTest {
        val sesion = SesionEnMemoria()
        val vm = LoginViewModel(DemoRepository(retardoMs = 0), sesion, CUENTAS_DEMO)
        vm.cambiarCorreo("rosa@umbral.pe")
        vm.cambiarClave(CLAVE_DEMO)
        vm.ingresar()
        advanceUntilIdle()
        assertEquals(Rol.APODERADO, sesion.actual?.usuario?.rol)
        assertNull(vm.estado.value.error)
    }

    @Test
    fun loginConClaveIncorrectaMuestraErrorYNoGuardaSesion() = runTest {
        val sesion = SesionEnMemoria()
        val vm = LoginViewModel(DemoRepository(retardoMs = 0), sesion, CUENTAS_DEMO)
        vm.cambiarCorreo("camila@umbral.pe")
        vm.cambiarClave("incorrecta")
        vm.ingresar()
        advanceUntilIdle()
        assertEquals("Correo o contraseña incorrectos.", vm.estado.value.error)
        assertNull(sesion.actual)
    }

    @Test
    fun sinCorreoNoSePuedeIngresar() {
        val vm = LoginViewModel(DemoRepository(retardoMs = 0), SesionEnMemoria(), CUENTAS_DEMO)
        vm.cambiarClave(CLAVE_DEMO)
        assertFalse(vm.estado.value.puedeIngresar)
    }

    // ---- Tendencia de simulacros (apoderado) ----
    private fun resultados(vararg puntajes: Int) =
        puntajes.mapIndexed { i, p -> ResultadoSimulacro("Simulacro ${i + 1}", "2026-11-0${i + 1}", p) }

    @Test
    fun tendenciaSegunLosDosUltimosSimulacros() {
        assertEquals(Tendencia.SUBE, MisHijosUiState(resultados = resultados(50, 60)).tendencia)
        assertEquals(Tendencia.BAJA, MisHijosUiState(resultados = resultados(70, 52)).tendencia)
        assertEquals(Tendencia.ESTABLE, MisHijosUiState(resultados = resultados(60, 60)).tendencia)
        assertEquals(Tendencia.SIN_DATOS, MisHijosUiState(resultados = resultados(60)).tendencia)
    }
}
