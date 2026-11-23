package com.umbral.app.ui.docente

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.Clase
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.TituloSeccion
import com.umbral.app.ui.common.vmFactory
import com.umbral.app.ui.theme.Fondo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/** Clases de un día del horario (dia: 1 = lunes ... 6 = sábado). */
data class DiaClases(val dia: Int, val clases: List<Clase>)

data class HoyUiState(
    val cargando: Boolean = true,
    val clasesHoy: List<Clase> = emptyList(),
    val semana: List<DiaClases> = emptyList(),
)

class HoyViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(HoyUiState())
    val estado: StateFlow<HoyUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)
            val clases = repo.horarioSemanal()
            // dayOfWeek: 1 = lunes ... 7 = domingo. Domingo no coincide con ningún día del horario.
            val hoy = ((Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1 // 1 = lunes ... 7 = domingo
            val semana = clases
                .groupBy { it.dia }
                .toList()
                .sortedBy { it.first }
                .map { (dia, lista) -> DiaClases(dia, lista.sortedBy { it.horaInicio }) }
            _estado.value = HoyUiState(
                cargando = false,
                clasesHoy = semana.firstOrNull { it.dia == hoy }?.clases ?: emptyList(),
                semana = semana,
            )
        }
    }
}

private val nombresDia = listOf("", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

@Composable
fun HoyScreen(repo: UmbralRepository, nombre: String, onSalir: () -> Unit, modifier: Modifier = Modifier) {
    val vm: HoyViewModel = viewModel<HoyViewModel>(factory = vmFactory { HoyViewModel(repo) })
    val estado by vm.estado.collectAsState()

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
