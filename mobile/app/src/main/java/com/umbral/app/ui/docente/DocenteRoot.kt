package com.umbral.app.ui.docente

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.umbral.app.domain.Usuario
import com.umbral.app.ui.navegacion.ContenedorConPestanas
import com.umbral.app.ui.navegacion.Pestana

private val PESTANAS_DOCENTE = listOf(
    Pestana("hoy", "🗓️", "Hoy"),
    Pestana("alumnos", "👥", "Alumnos"),
    Pestana("dudas", "💬", "Dudas"),
)

@Composable
fun DocenteRoot(usuario: Usuario, onSalir: () -> Unit) {
    val nav = rememberNavController()
    ContenedorConPestanas(nav, PESTANAS_DOCENTE) { mod ->
        NavHost(navController = nav, startDestination = "hoy", modifier = mod) {
            composable("hoy") { HoyScreen(nombre = usuario.nombre, onSalir = onSalir) }
            composable("alumnos") { AlumnosScreen() }
            composable("dudas") { DudasScreen() }
        }
    }
}
