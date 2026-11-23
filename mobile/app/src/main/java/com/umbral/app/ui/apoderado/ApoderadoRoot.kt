package com.umbral.app.ui.apoderado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umbral.app.domain.Hijo
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.domain.Usuario

private data class DestinoApoderado(val icono: String, val titulo: String)

private val destinosApoderado = listOf(
    DestinoApoderado("👨‍👩‍👧", "Mis hijos"),
    DestinoApoderado("🔔", "Avisos"),
    DestinoApoderado("📅", "Simulacros"),
)

@Composable
fun ApoderadoRoot(repo: UmbralRepository, usuario: Usuario, onSalir: () -> Unit) {
    var destino by rememberSaveable { mutableIntStateOf(0) }
    // -1 = todavía no hay hijo elegido.
    var hijoId by rememberSaveable { mutableIntStateOf(-1) }
    var hijos by remember { mutableStateOf<List<Hijo>>(emptyList()) }

    LaunchedEffect(Unit) {
        hijos = repo.hijos()
        if (hijoId < 0 && hijos.isNotEmpty()) hijoId = hijos.first().id
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinosApoderado.forEachIndexed { i, d ->
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
            0 -> MisHijosScreen(
                repo = repo,
                nombre = usuario.nombre,
                hijos = hijos,
                hijoId = hijoId,
                onElegirHijo = { hijoId = it },
                onSalir = onSalir,
                modifier = mod,
            )
            1 -> AvisosScreen(repo = repo, modifier = mod)
            else -> SimulacrosScreen(
                repo = repo,
                hijos = hijos,
                hijoId = hijoId,
                onElegirHijo = { hijoId = it },
                modifier = mod,
            )
        }
    }
}

/** Fila de botones para elegir hijo. El elegido usa Button; los demás, OutlinedButton. */
@Composable
fun SelectorHijos(
    hijos: List<Hijo>,
    hijoId: Int,
    onElegir: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(hijos, key = { it.id }) { h ->
            if (h.id == hijoId) {
                Button(onClick = { onElegir(h.id) }) { Text(h.nombre) }
            } else {
                OutlinedButton(onClick = { onElegir(h.id) }) { Text(h.nombre) }
            }
        }
    }
}
