package com.umbral.app.ui.apoderado

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.umbral.app.ui.common.ErrorConReintento
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.AvisoInasistencia
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.Etiqueta
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.theme.Fondo
import com.umbral.app.ui.theme.Luz
import com.umbral.app.ui.theme.Tinta

@Composable
fun AvisosScreen(modifier: Modifier = Modifier) {
    val vm: AvisosViewModel = viewModel(factory = AvisosViewModel.Factory)
    val estado by vm.estado.collectAsStateWithLifecycle()

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
            val errorCarga = estado.error
            if (errorCarga != null) {
                item { ErrorConReintento(errorCarga, onReintentar = { vm.cargar() }) }
            }
            if (!estado.cargando && estado.error == null && estado.avisos.isEmpty()) {
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
