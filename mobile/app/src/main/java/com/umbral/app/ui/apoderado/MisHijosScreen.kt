package com.umbral.app.ui.apoderado

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.umbral.app.ui.common.ErrorConReintento
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.EstadoAsistencia
import com.umbral.app.domain.Hijo
import com.umbral.app.domain.RegistroAsistencia
import com.umbral.app.domain.ResultadoSimulacro
import com.umbral.app.ui.common.Encabezado
import com.umbral.app.ui.common.Etiqueta
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.common.TarjetaDato
import com.umbral.app.ui.common.TituloSeccion
import com.umbral.app.ui.theme.AcentoOscuro
import com.umbral.app.ui.theme.ErrorRojo
import com.umbral.app.ui.theme.Fondo
import com.umbral.app.ui.theme.Luz
import com.umbral.app.ui.theme.Primario
import com.umbral.app.ui.theme.Tinta

@Composable
fun MisHijosScreen(
    nombre: String,
    hijos: List<Hijo>,
    hijoId: Int,
    onElegirHijo: (Int) -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: MisHijosViewModel = viewModel(factory = MisHijosViewModel.Factory)
    val estado by vm.estado.collectAsStateWithLifecycle()

    LaunchedEffect(hijoId) {
        if (hijoId >= 0) vm.cargar(hijoId)
    }

    val hijo = hijos.firstOrNull { it.id == hijoId }
    val listo = !estado.cargando && estado.hijoId == hijoId

    Column(modifier.fillMaxSize().background(Fondo)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSalir) { Text("Salir") }
        }
        Encabezado("Mis hijos", "Hola, $nombre")
        SelectorHijos(
            hijos = hijos,
            hijoId = hijoId,
            onElegir = onElegirHijo,
            modifier = Modifier.padding(vertical = 12.dp),
        )

        if (hijos.isEmpty()) {
            EstadoVacio("Sin hijos vinculados", "Pide a la institución que vincule a tu hijo con tu cuenta.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (estado.error != null) {
                    item { ErrorConReintento(estado.error.orEmpty(), onReintentar = { vm.cargar(hijoId, forzar = true) }) }
                } else if (!listo) {
                    item { Text("Cargando…") }
                } else {
                    item { TituloSeccion("Asistencia de ${hijo?.nombre ?: "tu hijo"}") }
                    item {
                        TarjetaDato(
                            valor = "${estado.porcentaje}%",
                            etiqueta = "de asistencia (presentes + tardanzas)",
                            modifier = Modifier.fillMaxWidth(),
                            color = AcentoOscuro,
                        )
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TarjetaDato(
                                valor = "${estado.presentes}",
                                etiqueta = "Presentes",
                                modifier = Modifier.weight(1f),
                                color = AcentoOscuro,
                            )
                            TarjetaDato(
                                valor = "${estado.tardanzas}",
                                etiqueta = "Tardanzas",
                                modifier = Modifier.weight(1f),
                                color = Tinta,
                            )
                            TarjetaDato(
                                valor = "${estado.faltas}",
                                etiqueta = "Faltas",
                                modifier = Modifier.weight(1f),
                                color = ErrorRojo,
                            )
                        }
                    }
                    if (estado.asistencia.isEmpty()) {
                        item { EstadoVacio("Sin registros", "Aún no hay asistencia registrada.") }
                    }
                    items(estado.asistencia) { r -> RegistroCard(r) }

                    item { TituloSeccion("Simulacros") }
                    item {
                        Tarjeta {
                            Text(
                                "Tendencia: ${estado.tendencia.texto}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    if (estado.resultados.isEmpty()) {
                        item { EstadoVacio("Sin simulacros rendidos", "Aquí aparecerán sus puntajes.") }
                    }
                    items(estado.resultados) { r -> ResultadoCard(r) }
                }
            }
        }
    }
}

@Composable
private fun RegistroCard(r: RegistroAsistencia) {
    Tarjeta {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(r.fecha, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            EtiquetaEstado(r.estado)
        }
    }
}

@Composable
private fun ResultadoCard(r: ResultadoSimulacro) {
    Tarjeta {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(r.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    r.fecha,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${r.puntaje} pts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Primario,
            )
        }
    }
}

@Composable
private fun EtiquetaEstado(estado: EstadoAsistencia) {
    when (estado) {
        EstadoAsistencia.PRESENTE -> Etiqueta(estado.texto, AcentoOscuro, Color.White)
        EstadoAsistencia.TARDANZA -> Etiqueta(estado.texto, Luz, Tinta)
        EstadoAsistencia.FALTA -> Etiqueta(estado.texto, ErrorRojo, Color.White)
    }
}
