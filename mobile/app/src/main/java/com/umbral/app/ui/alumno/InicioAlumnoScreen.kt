package com.umbral.app.ui.alumno

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.Clase
import com.umbral.app.domain.TemaDebil
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.domain.Usuario
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.TarjetaDato
import com.umbral.app.ui.common.TituloSeccion
import com.umbral.app.ui.common.vmFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InicioAlumnoUiState(
    val cargando: Boolean = true,
    val avancePct: Int = 0,
    val proximaClase: Clase? = null,
    val temasDebiles: List<TemaDebil> = emptyList(),
)

class InicioAlumnoViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _state = MutableStateFlow(InicioAlumnoUiState())
    val state: StateFlow<InicioAlumnoUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val progreso = repo.progreso()
            val totales = progreso.sumOf { it.totales }
            val completadas = progreso.sumOf { it.completadas }
            val pct = if (totales == 0) 0 else completadas * 100 / totales
            val proxima = repo.horarioSemanal().firstOrNull()
            val debiles = repo.temasDebiles().take(2)
            _state.value = InicioAlumnoUiState(
                cargando = false,
                avancePct = pct,
                proximaClase = proxima,
                temasDebiles = debiles,
            )
        }
    }
}

private val margenH = Modifier.padding(horizontal = 16.dp)

@Composable
fun InicioAlumnoScreen(
    repo: UmbralRepository,
    usuario: Usuario,
    onIrAPractica: () -> Unit,
    onSalir: () -> Unit,
) {
    val vm: InicioAlumnoViewModel = viewModel(factory = vmFactory { InicioAlumnoViewModel(repo) })
    val estado by vm.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onSalir) { Text("Salir") }
            }
        }
        item { Encabezado(titulo = "Hola, ${usuario.nombre}", subtitulo = "Tu avance de esta semana") }

        item {
            Column(modifier = margenH, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaDato(
                    valor = "${estado.avancePct}%",
                    etiqueta = "Avance semanal",
                    modifier = Modifier.fillMaxWidth(),
                )
                Tarjeta {
                    Text("Próxima clase", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    val clase = estado.proximaClase
                    if (clase == null) {
                        Text("No tienes clases programadas.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(clase.curso, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${clase.horaInicio} – ${clase.horaFin} · ${clase.aula}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            if (estado.cargando) {
                CircularProgressIndicator(modifier = margenH)
            }
        }

        item { TituloSeccion("Temas para reforzar", modifier = margenH) }

        if (!estado.cargando && estado.temasDebiles.isEmpty()) {
            item {
                EstadoVacio(
                    titulo = "Sin temas pendientes",
                    detalle = "Sigue practicando para ver tus avances.",
                    modifier = margenH,
                )
            }
        }

        items(estado.temasDebiles) { tema ->
            Tarjeta(modifier = margenH.clickable(onClick = onIrAPractica)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(tema.tema, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Acierto ${tema.aciertoPct}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("Practicar ›", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
