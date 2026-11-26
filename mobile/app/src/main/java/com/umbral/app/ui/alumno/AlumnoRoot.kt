package com.umbral.app.ui.alumno

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.umbral.app.domain.Usuario
import com.umbral.app.ui.navegacion.ContenedorConPestanas
import com.umbral.app.ui.navegacion.Pestana
import com.umbral.app.ui.navegacion.irAPestana

private val PESTANAS_ALUMNO = listOf(
    Pestana("inicio", "🏠", "Inicio"),
    Pestana("practica", "✏️", "Práctica"),
    Pestana("progreso", "📈", "Progreso"),
    Pestana("horario", "🗓️", "Horario"),
)

/** Contenedor del alumno: barra inferior con 4 destinos, cada uno con su ViewModel. */
@Composable
fun AlumnoRoot(usuario: Usuario, onSalir: () -> Unit) {
    val nav = rememberNavController()
    ContenedorConPestanas(nav, PESTANAS_ALUMNO) { mod ->
        NavHost(navController = nav, startDestination = "inicio", modifier = mod) {
            composable("inicio") {
                InicioAlumnoScreen(
                    usuario = usuario,
                    onIrAPractica = { nav.irAPestana("practica") },
                    onSalir = onSalir,
                )
            }
            composable("practica") { PracticaScreen() }
            composable("progreso") { ProgresoScreen() }
            composable("horario") { HorarioScreen() }
        }
    }
}
