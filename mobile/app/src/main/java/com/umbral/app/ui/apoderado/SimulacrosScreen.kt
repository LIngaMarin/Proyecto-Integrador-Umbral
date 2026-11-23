package com.umbral.app.ui.apoderado

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.Hijo
import com.umbral.app.domain.SimulacroProgramado
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.vmFactory
import com.umbral.app.ui.theme.Fondo
import com.umbral.app.ui.theme.Primario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SimulacrosUiState(
    val cargando: Boolean = false,
    val hijoId: Int = -1,
    val agenda: List<SimulacroProgramado> = emptyList(),
)

class SimulacrosViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(SimulacrosUiState())
    val estado: StateFlow<SimulacrosUiState> = _estado.asStateFlow()

    fun cargar(hijoId: Int) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, hijoId = hijoId)
            val lista = repo.simulacrosProgramados(hijoId)
                .sortedWith(compareBy<SimulacroProgramado>({ it.fecha }, { it.hora }))
            // Si mientras tanto se eligió otro hijo, este resultado ya no corresponde.
            if (_estado.value.hijoId != hijoId) return@launch
            _estado.value = _estado.value.copy(cargando = false, agenda = lista)
        }
    }
}

@Composable
fun SimulacrosScreen(
    repo: UmbralRepository,
    hijos: List<Hijo>,
    hijoId: Int,
    onElegirHijo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SimulacrosViewModel = viewModel<SimulacrosViewModel>(factory = vmFactory { SimulacrosViewModel(repo) })
    val estado by vm.estado.collectAsState()

    LaunchedEffect(hijoId) {
        if (hijoId >= 0) vm.cargar(hijoId)
    }

    val hijo = hijos.firstOrNull { it.id == hijoId }

    Column(modifier.fillMaxSize().background(Fondo)) {
        Encabezado("Simulacros", hijo?.let { "Agenda de ${it.nombre}" } ?: "Elige un hijo")
        SelectorHijos(
            hijos = hijos,
            hijoId = hijoId,
            onElegir = onElegirHijo,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (estado.agenda.isEmpty()) {
                item {
                    EstadoVacio(
                        if (estado.cargando) "Cargando…" else "Sin simulacros programados",
                        "Cuando el colegio programe uno, aparecerá aquí.",
                    )
                }
            } else {
                items(estado.agenda) { s -> AgendaCard(s) }
            }
        }
    }
}

@Composable
private fun AgendaCard(s: SimulacroProgramado) {
    Tarjeta {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                s.fecha,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Primario,
            )
            Column(Modifier.weight(1f)) {
                Text(s.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(s.hora, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Aula ${s.aula}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
