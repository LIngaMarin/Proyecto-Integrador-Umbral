package com.umbral.app.ui.docente

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

private data class DestinoDocente(val icono: String, val titulo: String)

private val destinosDocente = listOf(
    DestinoDocente("🗓️", "Hoy"),
    DestinoDocente("👥", "Alumnos"),
    DestinoDocente("💬", "Dudas"),
)

@Composable
fun DocenteRoot(repo: UmbralRepository, usuario: Usuario, onSalir: () -> Unit) {
    var destino by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinosDocente.forEachIndexed { i, d ->
                    NavigationBarItem(
                        selected = destino == i,
                        onClick = { destino = i },
                        icon = { Text(d.icono) },
                        label = { Text(d.titulo) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val mod = Modifier.padding(innerPadding)
        when (destino) {
            0 -> HoyScreen(repo = repo, nombre = usuario.nombre, onSalir = onSalir, modifier = mod)
            1 -> AlumnosScreen(repo = repo, modifier = mod)
            else -> DudasScreen(repo = repo, modifier = mod)
        }
    }
}
