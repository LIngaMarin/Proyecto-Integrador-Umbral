package com.umbral.app.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.umbral.app.data.local.PreguntasPracticasEntity
import com.umbral.app.data.local.SimulacroResultadoEntity
import com.umbral.app.ui.viewmodel.SimulacroUiState
import com.umbral.app.ui.viewmodel.SimulacroViewModel

@Composable
fun ResolverSimulacroContainer(
    viewModel: SimulacroViewModel,
    onVolverInicio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is SimulacroUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cargando simulacro...")
                }
            }
        }
        is SimulacroUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error al cargar el simulacro",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.mensaje)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onVolverInicio) {
                        Text("Volver")
                    }
                }
            }
        }
        is SimulacroUiState.Success -> {
            ResultadoSimulacroContent(
                resultado = state.resultado,
                onVolverInicio = onVolverInicio,
                modifier = modifier
            )
        }
        is SimulacroUiState.Resolver -> {
            ResolverSimulacroContent(
                state = state,
                onRespuestaSeleccionada = { pId, r -> viewModel.seleccionarRespuesta(pId, r) },
                onIrAPregunta = { idx -> viewModel.irAPregunta(idx) },
                onAnterior = { viewModel.anteriorPregunta() },
                onSiguiente = { viewModel.siguientePregunta() },
                onEnviar = { viewModel.enviarSimulacro() },
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolverSimulacroContent(
    state: SimulacroUiState.Resolver,
    onRespuestaSeleccionada: (Long, String) -> Unit,
    onIrAPregunta: (Int) -> Unit,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onEnviar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preguntaActual = state.preguntaActual

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Simulacro Tipo Admisión")
                        Text(
                            text = "Pregunta ${state.indiceActual + 1} de ${state.preguntas.size}  •  ⏱ ${formatTiempo(state.tiempoSegundos)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Barra de banderas / navegación rápida de preguntas
                Text(
                    text = "Banderas de estado:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(state.preguntas) { index, preg ->
                        val respondida = state.respuestasMap.containsKey(preg.id)
                        val esActual = index == state.indiceActual

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        esActual -> MaterialTheme.colorScheme.primary
                                        respondida -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable { onIrAPregunta(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (esActual) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    esActual -> MaterialTheme.colorScheme.onPrimary
                                    respondida -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (preguntaActual != null) {
                    val respuestaActual = state.respuestasMap[preguntaActual.id] ?: ""
                    val opciones = listOf(
                        "A" to preguntaActual.opcionA,
                        "B" to preguntaActual.opcionB,
                        "C" to preguntaActual.opcionC,
                        "D" to preguntaActual.opcionD
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Tema: ${preguntaActual.tema}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = preguntaActual.enunciado,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        opciones.forEach { (clave, texto) ->
                            val selected = (respuestaActual == clave)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .selectable(
                                        selected = selected,
                                        onClick = { onRespuestaSeleccionada(preguntaActual.id, clave) },
                                        role = Role.RadioButton
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null
                                )
                                Text(
                                    text = "$clave) $texto",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }

                if (state.errorEnvio != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.errorEnvio,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Barra inferior de controles
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onAnterior,
                        enabled = state.indiceActual > 0 && !state.enviando
                    ) {
                        Text("Anterior")
                    }

                    if (state.esUltimaPregunta) {
                        Button(
                            onClick = onEnviar,
                            enabled = !state.enviando,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (state.enviando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enviando...")
                            } else {
                                Text("Finalizar y Enviar")
                            }
                        }
                    } else {
                        Button(
                            onClick = onSiguiente,
                            enabled = !state.enviando
                        ) {
                            Text("Siguiente")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadoSimulacroContent(
    resultado: SimulacroResultadoEntity,
    onVolverInicio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultado del Simulacro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🎉 ¡Simulacro Enviado!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = String.format(java.util.Locale.getDefault(), "%.1f / 20", resultado.puntajeObtenido),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = resultado.tituloSimulacro,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "Fecha: ${resultado.fechaRealizacion}")
                        Text(
                            text = "✅ Correctas: ${resultado.respuestasCorrectas}",
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "❌ Incorrectas: ${resultado.respuestasIncorrectas}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "⚪ En blanco: ${resultado.respuestasEnBlanco}",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Button(
                onClick = onVolverInicio,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al Inicio")
            }
        }
    }
}

// Sobrecarga de conveniencia para la previsualización existente
@Composable
fun ResolverSimulacroScreen(
    preguntaActual: PreguntasPracticasEntity,
    numeroPregunta: Int,
    totalPreguntas: Int,
    onRespuestaSeleccionada: (String) -> Unit,
    onSiguientePregunta: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = SimulacroUiState.Resolver(
        preguntas = listOf(preguntaActual),
        indiceActual = 0,
        respuestasMap = emptyMap(),
        tiempoSegundos = 125
    )

    ResolverSimulacroContent(
        state = state,
        onRespuestaSeleccionada = { _, r -> onRespuestaSeleccionada(r) },
        onIrAPregunta = { },
        onAnterior = { },
        onSiguiente = onSiguientePregunta,
        onEnviar = onSiguientePregunta,
        modifier = modifier
    )
}

private fun formatTiempo(segundosTotales: Int): String {
    val minutos = segundosTotales / 60
    val segundos = segundosTotales % 60
    return String.format("%02d:%02d", minutos, segundos)
}
