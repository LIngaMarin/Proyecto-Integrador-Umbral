package com.umbral.app.ui.alumno

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.domain.Usuario

private class DestinoAlumno(val emoji: String, val etiqueta: String)

private val destinosAlumno = listOf(
    DestinoAlumno("🏠", "Inicio"),
    DestinoAlumno("✏️", "Práctica"),
    DestinoAlumno("📈", "Progreso"),
    DestinoAlumno("🗓️", "Horario"),
)

/** Contenedor del alumno: barra inferior con 4 destinos. */
@Composable
fun AlumnoRoot(repo: UmbralRepository, usuario: Usuario, onSalir: () -> Unit) {
    var destino by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinosAlumno.forEachIndexed { indice, d ->
                    NavigationBarItem(
                        selected = destino == indice,
                        onClick = { destino = indice },
                        icon = { Text(d.emoji) },
                        label = { Text(d.etiqueta) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (destino) {
                0 -> InicioAlumnoScreen(repo = repo, usuario = usuario, onIrAPractica = { destino = 1 }, onSalir = onSalir)
                1 -> PracticaScreen(repo = repo)
                2 -> ProgresoScreen(repo = repo)
                else -> HorarioScreen(repo = repo)
            }
        }
    }
}
