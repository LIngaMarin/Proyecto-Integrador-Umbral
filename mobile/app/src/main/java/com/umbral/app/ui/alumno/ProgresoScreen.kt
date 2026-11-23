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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.Comparativo
import com.umbral.app.domain.ProgresoCurso
import com.umbral.app.domain.TemaDebil
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.BarraProgreso
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.TituloSeccion
import com.umbral.app.ui.common.vmFactory
import com.umbral.app.ui.theme.Acento
import com.umbral.app.ui.theme.ErrorRojo
import com.umbral.app.ui.theme.Tinta
import com.umbral.app.ui.theme.TextoSuave
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProgresoUiState(
    val cargando: Boolean = true,
    val cursos: List<ProgresoCurso> = emptyList(),
    val temasDebiles: List<TemaDebil> = emptyList(),
    val comparativo: Comparativo? = null,
)

class ProgresoViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProgresoUiState())
    val state: StateFlow<ProgresoUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cursos = repo.progreso()
            val debiles = repo.temasDebiles()
            val comp = repo.comparativo()
            _state.value = ProgresoUiState(
                cargando = false,
                cursos = cursos,
                temasDebiles = debiles,
                comparativo = comp,
            )
        }
    }
}

private val margenH = Modifier.padding(horizontal = 16.dp)

@Composable
fun ProgresoScreen(repo: UmbralRepository) {
    val vm: ProgresoViewModel = viewModel(factory = vmFactory { ProgresoViewModel(repo) })
    val estado by vm.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Encabezado(titulo = "Progreso", subtitulo = "Cómo vas en tus cursos y temas") }

        if (estado.cargando) {
            item { CircularProgressIndicator(modifier = margenH) }
        }

        item { TituloSeccion("Avance por curso", modifier = margenH) }
        items(estado.cursos) { curso ->
            val fraccion = if (curso.totales == 0) 0f else curso.completadas.toFloat() / curso.totales
            Tarjeta(modifier = margenH) {
                BarraProgreso(
                    etiqueta = "${curso.curso} (${curso.completadas} de ${curso.totales})",
                    fraccion = fraccion,
                    color = Acento,
                )
            }
        }

        item { TituloSeccion("Temas por reforzar", modifier = margenH) }
        if (!estado.cargando && estado.temasDebiles.isEmpty()) {
            item {
                EstadoVacio(
                    titulo = "Sin temas débiles",
                    detalle = "Cuando practiques más, verás aquí los temas a reforzar.",
                    modifier = margenH,
                )
            }
        }
        items(estado.temasDebiles) { tema ->
            Tarjeta(modifier = margenH) {
                BarraProgreso(
                    etiqueta = tema.tema,
                    fraccion = tema.aciertoPct / 100f,
                    color = ErrorRojo,
                )
            }
        }

        item { TituloSeccion("Comparativo", modifier = margenH) }
        estado.comparativo?.let { comp ->
            item {
                Tarjeta(modifier = margenH) {
                    BarraProgreso(
                        etiqueta = "Mi promedio",
                        fraccion = comp.miPromedio / 100f,
                        color = Acento,
                    )
                    BarraProgreso(
                        etiqueta = "Promedio de tu sección",
                        fraccion = comp.promedioSeccion / 100f,
                        color = Tinta,
                    )
                    Text(
                        "El promedio de tu sección es anónimo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuave,
                    )
                }
            }
        }
    }
}
