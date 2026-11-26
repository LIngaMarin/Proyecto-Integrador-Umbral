package com.umbral.app.ui.apoderado

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.umbral.app.domain.Hijo
import com.umbral.app.domain.Usuario
import com.umbral.app.ui.common.ErrorConReintento
import com.umbral.app.ui.common.PantallaCarga
import com.umbral.app.ui.navegacion.ContenedorConPestanas
import com.umbral.app.ui.navegacion.Pestana

private val PESTANAS_APODERADO = listOf(
    Pestana("hijos", "👨‍👩‍👧", "Mis hijos"),
    Pestana("avisos", "🔔", "Avisos"),
    Pestana("simulacros", "📅", "Simulacros"),
)

/**
 * Contenedor del apoderado. Los hijos y el hijo elegido viven en ApoderadoViewModel
 * (compartido por las pestañas), ya no en la pantalla.
 */
@Composable
fun ApoderadoRoot(usuario: Usuario, onSalir: () -> Unit) {
    val vm: ApoderadoViewModel = viewModel(factory = ApoderadoViewModel.Factory)
    val estado by vm.estado.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    ContenedorConPestanas(nav, PESTANAS_APODERADO) { mod ->
        val error = estado.error
        when {
            estado.cargando && estado.hijos.isEmpty() -> PantallaCarga(mod)
            error != null && estado.hijos.isEmpty() -> Box(mod.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                ErrorConReintento(error, onReintentar = { vm.cargarHijos() })
            }
            else -> NavHost(navController = nav, startDestination = "hijos", modifier = mod) {
                composable("hijos") {
                    MisHijosScreen(
                        nombre = usuario.nombre,
                        hijos = estado.hijos,
                        hijoId = estado.hijoId,
                        onElegirHijo = vm::elegirHijo,
                        onSalir = onSalir,
                    )
                }
                composable("avisos") { AvisosScreen() }
                composable("simulacros") {
                    SimulacrosScreen(
                        hijos = estado.hijos,
                        hijoId = estado.hijoId,
                        onElegirHijo = vm::elegirHijo,
                    )
                }
            }
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
