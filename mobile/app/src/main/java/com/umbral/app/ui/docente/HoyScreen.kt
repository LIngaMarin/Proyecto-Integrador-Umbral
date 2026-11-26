package com.umbral.app.ui.docente

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.umbral.app.ui.common.ErrorConReintento
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.Clase
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.TituloSeccion
import com.umbral.app.ui.theme.Fondo

private val nombresDia = listOf("", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

@Composable
fun HoyScreen(nombre: String, onSalir: () -> Unit, modifier: Modifier = Modifier) {
    val vm: HoyViewModel = viewModel(factory = HoyViewModel.Factory)
    val estado by vm.estado.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().background(Fondo)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSalir) { Text("Salir") }
        }
        Encabezado("Hoy", "Hola, $nombre")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (estado.cargando) {
                item { Text("Cargando…") }
            } else if (estado.error != null) {
                item { ErrorConReintento(estado.error.orEmpty(), onReintentar = { vm.cargar() }) }
            } else if (estado.clasesHoy.isNotEmpty()) {
                item { TituloSeccion("Clases de hoy") }
                items(estado.clasesHoy) { c -> ClaseCard(c) }
            } else {
                item { EstadoVacio("Hoy no dictas clases", "Esta es tu próxima semana completa.") }
                estado.semana.forEach { dc ->
                    item { TituloSeccion(nombresDia.getOrElse(dc.dia) { "Día ${dc.dia}" }) }
                    items(dc.clases) { c -> ClaseCard(c) }
                }
            }
        }
    }
}

@Composable
private fun ClaseCard(c: Clase) {
    Tarjeta {
        Text(c.curso, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Sección ${c.seccion} · ${c.aula} · ${c.sede}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("${c.horaInicio} – ${c.horaFin}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}
