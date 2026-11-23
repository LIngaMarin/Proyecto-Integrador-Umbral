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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.AlumnoSeccion
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.Etiqueta
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.vmFactory
import com.umbral.app.ui.theme.AcentoOscuro
import com.umbral.app.ui.theme.ErrorRojo
import com.umbral.app.ui.theme.Fondo
import com.umbral.app.ui.theme.Luz
import com.umbral.app.ui.theme.Tinta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlumnosUiState(
    val cargando: Boolean = true,
    val todos: List<AlumnoSeccion> = emptyList(),
    val busqueda: String = "",
) {
    val visibles: List<AlumnoSeccion>
        get() = todos.filter { it.nombre.contains(busqueda.trim(), ignoreCase = true) }
}

class AlumnosViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AlumnosUiState())
    val estado: StateFlow<AlumnosUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)
            val lista = repo.alumnosSeccion()
            _estado.value = _estado.value.copy(cargando = false, todos = lista)
        }
    }

    fun cambiarBusqueda(texto: String) {
        _estado.value = _estado.value.copy(busqueda = texto)
    }
}

@Composable
fun AlumnosScreen(repo: UmbralRepository, modifier: Modifier = Modifier) {
    val vm: AlumnosViewModel = viewModel<AlumnosViewModel>(factory = vmFactory { AlumnosViewModel(repo) })
    val estado by vm.estado.collectAsState()
    val visibles = estado.visibles

    Column(modifier.fillMaxSize().background(Fondo)) {
        Encabezado("Alumnos", "${estado.todos.size} alumnos en tus secciones")
        OutlinedTextField(
            value = estado.busqueda,
            onValueChange = { vm.cambiarBusqueda(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            label = { Text("Buscar alumno") },
            singleLine = true,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!estado.cargando && visibles.isEmpty()) {
                item {
                    EstadoVacio(
                        if (estado.todos.isEmpty()) "Sin alumnos" else "Sin coincidencias",
                        if (estado.todos.isEmpty()) "Aún no hay alumnos en tus secciones." else "Prueba con otro nombre.",
                    )
                }
            }
            items(visibles) { a -> AlumnoCard(a) }
        }
    }
}

@Composable
private fun AlumnoCard(a: AlumnoSeccion) {
    Tarjeta {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(a.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Sección ${a.seccion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AsistenciaEtiqueta(a.asistenciaPct)
        }
    }
}

@Composable
private fun AsistenciaEtiqueta(pct: Int) {
    val (fondo, texto) = when {
        pct >= 90 -> AcentoOscuro to Color.White
        pct >= 80 -> Luz to Tinta
        else -> ErrorRojo to Color.White
    }
    Etiqueta("$pct%", fondo, texto)
}
