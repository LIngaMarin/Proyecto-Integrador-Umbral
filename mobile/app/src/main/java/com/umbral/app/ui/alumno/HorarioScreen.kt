package com.umbral.app.ui.alumno

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.umbral.app.ui.common.ErrorConReintento
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
import com.umbral.app.ui.theme.AcentoOscuro

private val margenH = Modifier.padding(horizontal = 16.dp)

@Composable
fun HorarioScreen() {
    val vm: HorarioViewModel = viewModel(factory = HorarioViewModel.Factory)
    val estado by vm.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Encabezado(titulo = "Horario", subtitulo = "Tus clases de la semana") }
        val errorCarga = estado.error
        if (errorCarga != null) {
            item { ErrorConReintento(errorCarga, onReintentar = { vm.cargar() }, modifier = margenH) }
        }

        if (estado.cargando) {
            item { CircularProgressIndicator(modifier = margenH) }
        }

        if (!estado.cargando && estado.error == null && estado.dias.isEmpty()) {
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
