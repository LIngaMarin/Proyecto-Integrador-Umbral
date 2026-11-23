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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.Duda
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.Etiqueta
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.vmFactory
import com.umbral.app.ui.theme.AcentoOscuro
import com.umbral.app.ui.theme.Fondo
import com.umbral.app.ui.theme.Luz
import com.umbral.app.ui.theme.Tinta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DudasUiState(
    val cargando: Boolean = true,
    val dudas: List<Duda> = emptyList(),
    val soloPendientes: Boolean = true,
    val enviandoId: Int? = null,
) {
    val visibles: List<Duda>
        get() = if (soloPendientes) dudas.filter { !it.resuelta } else dudas
}

class DudasViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(DudasUiState())
    val estado: StateFlow<DudasUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)
            val lista = repo.dudas()
            _estado.value = _estado.value.copy(cargando = false, dudas = lista)
        }
    }

    fun elegirFiltro(soloPendientes: Boolean) {
        _estado.value = _estado.value.copy(soloPendientes = soloPendientes)
    }

    fun responder(id: Int, texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return
        viewModelScope.launch {
            _estado.value = _estado.value.copy(enviandoId = id)
            try {
                repo.responderDuda(id, limpio)
            } finally {
                _estado.value = _estado.value.copy(enviandoId = null)
            }
            cargar()
        }
    }
}

@Composable
fun DudasScreen(repo: UmbralRepository, modifier: Modifier = Modifier) {
    val vm: DudasViewModel = viewModel<DudasViewModel>(factory = vmFactory { DudasViewModel(repo) })
    val estado by vm.estado.collectAsState()
    val visibles = estado.visibles

    Column(modifier.fillMaxSize().background(Fondo)) {
        Encabezado("Dudas", "Responde las dudas de tus alumnos")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FiltroBoton("Pendientes", estado.soloPendientes) { vm.elegirFiltro(true) }
            FiltroBoton("Todas", !estado.soloPendientes) { vm.elegirFiltro(false) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (visibles.isEmpty()) {
                item {
                    EstadoVacio(
                        if (estado.cargando) "Cargando…" else if (estado.soloPendientes) "No hay dudas pendientes" else "Aún no hay dudas",
                        "Las dudas que envíen tus alumnos aparecerán aquí.",
                    )
                }
            }
            items(visibles, key = { it.id }) { duda ->
                DudaCard(
                    duda = duda,
                    enviando = estado.enviandoId == duda.id,
                    onResponder = { texto -> vm.responder(duda.id, texto) },
                )
            }
        }
    }
}

@Composable
private fun FiltroBoton(texto: String, activo: Boolean, onClick: () -> Unit) {
    if (activo) {
        Button(onClick = onClick) { Text(texto) }
    } else {
        OutlinedButton(onClick = onClick) { Text(texto) }
    }
}

@Composable
private fun DudaCard(duda: Duda, enviando: Boolean, onResponder: (String) -> Unit) {
    var respuesta by rememberSaveable(duda.id) { mutableStateOf("") }

    Tarjeta {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(duda.alumno, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            EstadoDuda(duda.resuelta)
        }
        Text(duda.tema, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(duda.texto, style = MaterialTheme.typography.bodyMedium)

        if (duda.resuelta) {
            val r = duda.respuesta
            Text(
                if (r.isNullOrBlank()) "Sin respuesta registrada" else "Tu respuesta: $r",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            OutlinedTextField(
                value = respuesta,
                onValueChange = { respuesta = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Escribe tu respuesta") },
                enabled = !enviando,
                minLines = 2,
            )
            Button(
                onClick = {
                    onResponder(respuesta)
                    respuesta = ""
                },
                enabled = respuesta.isNotBlank() && !enviando,
            ) {
                Text("Responder")
            }
        }
    }
}

@Composable
private fun EstadoDuda(resuelta: Boolean) {
    if (resuelta) {
        Etiqueta("Resuelta", AcentoOscuro, Color.White)
    } else {
        Etiqueta("Pendiente", Luz, Tinta)
    }
}
