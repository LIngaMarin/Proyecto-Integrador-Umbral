package com.umbral.app.ui.apoderado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.umbral.app.domain.AvisoInasistencia
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.Etiqueta
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.vmFactory
import com.umbral.app.ui.theme.Fondo
import com.umbral.app.ui.theme.Luz
import com.umbral.app.ui.theme.Tinta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AvisosUiState(
    val cargando: Boolean = true,
    val avisos: List<AvisoInasistencia> = emptyList(),
) {
    val sinLeer: Int
        get() = avisos.count { !it.leida }
}

class AvisosViewModel(private val repo: UmbralRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AvisosUiState())
    val estado: StateFlow<AvisosUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)
            // Se asume que fecha es ISO (aaaa-mm-dd), así el orden de texto es cronológico.
            val lista = repo.avisos().sortedByDescending { it.fecha }
            _estado.value = _estado.value.copy(cargando = false, avisos = lista)
        }
    }

    fun marcarLeido(id: Int) {
        viewModelScope.launch {
            repo.marcarAvisoLeido(id)
            cargar()
        }
    }
}

@Composable
fun AvisosScreen(repo: UmbralRepository, modifier: Modifier = Modifier) {
    val vm: AvisosViewModel = viewModel<AvisosViewModel>(factory = vmFactory { AvisosViewModel(repo) })
    val estado by vm.estado.collectAsState()

    Column(modifier.fillMaxSize().background(Fondo)) {
        Encabezado(
            "Avisos",
            if (estado.sinLeer > 0) "Tienes ${estado.sinLeer} sin leer" else "Todo al día",
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!estado.cargando && estado.avisos.isEmpty()) {
                item { EstadoVacio("Sin avisos", "Aquí llegarán los avisos de inasistencia de tus hijos.") }
            }
            items(estado.avisos, key = { it.id }) { a ->
                AvisoCard(aviso = a, onClick = { if (!a.leida) vm.marcarLeido(a.id) })
            }
        }
    }
}

@Composable
private fun AvisoCard(aviso: AvisoInasistencia, onClick: () -> Unit) {
    Tarjeta(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                aviso.fecha,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!aviso.leida) {
                Etiqueta("Nuevo", Luz, Tinta)
            }
        }
        Text(
            aviso.mensaje,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (aviso.leida) FontWeight.Normal else FontWeight.Bold,
        )
    }
}
