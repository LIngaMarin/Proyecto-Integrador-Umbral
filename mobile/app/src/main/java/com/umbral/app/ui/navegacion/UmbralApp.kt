package com.umbral.app.ui.navegacion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.umbral.app.domain.Rol
import com.umbral.app.ui.alumno.AlumnoRoot
import com.umbral.app.ui.apoderado.ApoderadoRoot
import com.umbral.app.ui.common.PantallaCarga
import com.umbral.app.ui.docente.DocenteRoot
import com.umbral.app.ui.login.LoginScreen
import com.umbral.app.ui.theme.Tinta

/** Rutas principales: el login y un grafo por cada rol. */
sealed class Ruta(val ruta: String) {
    data object Login : Ruta("login")
    data object Alumno : Ruta("alumno")
    data object Docente : Ruta("docente")
    data object Apoderado : Ruta("apoderado")
}

private fun rutaDe(rol: Rol): String = when (rol) {
    Rol.ALUMNO -> Ruta.Alumno.ruta
    Rol.DOCENTE -> Ruta.Docente.ruta
    Rol.APODERADO -> Ruta.Apoderado.ruta
}

private fun rutaPara(estado: EstadoSesion): String =
    if (estado is EstadoSesion.ConSesion) rutaDe(estado.usuario.rol) else Ruta.Login.ruta

@Composable
fun UmbralApp(sesionVm: SesionViewModel = viewModel(factory = SesionViewModel.Factory)) {
    val estado by sesionVm.estado.collectAsStateWithLifecycle()
    val actual = estado

    if (actual is EstadoSesion.Leyendo) {
        PantallaCarga(Modifier.background(Tinta))
        return
    }

    val navController = rememberNavController()
    // Se decide una sola vez: si ya habia sesion guardada, entra directo a su rol.
    val inicio = remember { rutaPara(actual) }

    NavHost(navController = navController, startDestination = inicio) {
        composable(Ruta.Login.ruta) { LoginScreen() }
        composable(Ruta.Alumno.ruta) {
            (estado as? EstadoSesion.ConSesion)?.let { AlumnoRoot(it.usuario, onSalir = sesionVm::cerrarSesion) }
        }
        composable(Ruta.Docente.ruta) {
            (estado as? EstadoSesion.ConSesion)?.let { DocenteRoot(it.usuario, onSalir = sesionVm::cerrarSesion) }
        }
        composable(Ruta.Apoderado.ruta) {
            (estado as? EstadoSesion.ConSesion)?.let { ApoderadoRoot(it.usuario, onSalir = sesionVm::cerrarSesion) }
        }
    }

    // Cuando cambia la sesion (login, logout o token vencido) se navega y se limpia la pila:
    // asi el boton Atras no regresa a la pantalla de otro usuario y sus ViewModels se destruyen.
    LaunchedEffect(actual) {
        val destino = rutaPara(actual)
        if (navController.currentDestination?.route != destino) {
            navController.navigate(destino) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

/** Una pestaña de la barra inferior de un rol. */
data class Pestana(val ruta: String, val icono: String, val etiqueta: String)

/** Scaffold con barra inferior: la usan los tres roles. */
@Composable
fun ContenedorConPestanas(
    navController: NavHostController,
    pestanas: List<Pestana>,
    contenido: @Composable (Modifier) -> Unit,
) {
    val entrada by navController.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                pestanas.forEach { p ->
                    NavigationBarItem(
                        selected = rutaActual == p.ruta,
                        onClick = { navController.irAPestana(p.ruta) },
                        icon = { Text(p.icono) },
                        label = { Text(p.etiqueta) },
                    )
                }
            }
        },
    ) { innerPadding ->
        contenido(Modifier.padding(innerPadding))
    }
}

/** Navega a una pestaña sin apilar copias y recordando el estado de cada una. */
fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
