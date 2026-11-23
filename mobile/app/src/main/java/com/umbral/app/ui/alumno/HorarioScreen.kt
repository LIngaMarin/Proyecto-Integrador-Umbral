package com.umbral.app.ui.alumno

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.umbral.app.ui.theme.AcentoOscuro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiaClases(val nombre: String, val clases: List<Clase>)

data class HorarioUiState(
    val cargando: Boolean = true,
    val dias: List<DiaClases> = emptyList(),
)

private val nombresDia = listOf("", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

class HorarioViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _state = MutableStateFlow(HorarioUiState())
    val state: StateFlow<HorarioUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val clases = repo.horarioSemanal()
            val dias = (1..6).mapNotNull { dia ->
                val delDia = clases.filter { it.dia == dia }.sortedBy { it.horaInicio }
                if (delDia.isEmpty()) null else DiaClases(nombresDia[dia], delDia)
            }
            _state.value = HorarioUiState(cargando = false, dias = dias)
        }
    }
}

private val margenH = Modifier.padding(horizontal = 16.dp)

@Composable
fun HorarioScreen(repo: UmbralRepository) {
    val vm: HorarioViewModel = viewModel(factory = vmFactory { HorarioViewModel(repo) })
    val estado by vm.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Encabezado(titulo = "Horario", subtitulo = "Tus clases de la semana") }

        if (estado.cargando) {
            item { CircularProgressIndicator(modifier = margenH) }
        }

        if (!estado.cargando && estado.dias.isEmpty()) {
            item {
                EstadoVacio(
                    titulo = "Sin clases programadas",
                    detalle = "Cuando tu docente publique el horario, lo verás aquí.",
                    modifier = margenH,
                )
            }
        }

        estado.dias.forEach { dia ->
            item { TituloSeccion(dia.nombre, modifier = margenH) }
            items(dia.clases) { clase ->
                Tarjeta(modifier = margenH) {
                    Text(
                        "${clase.horaInicio} – ${clase.horaFin}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AcentoOscuro,
                    )
                    Text(clase.curso, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sección ${clase.seccion} · ${clase.aula} · ${clase.sede}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Docente: ${clase.docente}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
