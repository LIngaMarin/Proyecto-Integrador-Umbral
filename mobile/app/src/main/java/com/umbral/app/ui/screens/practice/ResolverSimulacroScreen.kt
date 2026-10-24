package com.umbral.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.umbral.app.data.local.PreguntasPracticasEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolverSimulacroScreen(
    preguntaActual: PreguntasPracticasEntity,
    numeroPregunta: Int,
    totalPreguntas: Int,
    onRespuestaSeleccionada: (String) -> Unit,
    onSiguientePregunta: () -> Unit,
    modifier: Modifier = Modifier
) {
    var opcionSeleccionada by remember { mutableStateOf("") }
    val opciones = listOf(
        "A" to preguntaActual.opcionA,
        "B" to preguntaActual.opcionB,
        "C" to preguntaActual.opcionC,
        "D" to preguntaActual.opcionD
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulacro Tipo Admisión ($numeroPregunta/$totalPreguntas)") },
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
            Column {
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
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (opcionSeleccionada == clave),
                                    onClick = {
                                        opcionSeleccionada = clave
                                        onRespuestaSeleccionada(clave)
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (opcionSeleccionada == clave),
                                onClick = null
                            )
                            Text(
                                text = "$clave) $texto",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onSiguientePregunta,
                modifier = Modifier.fillMaxWidth(),
                enabled = opcionSeleccionada.isNotEmpty()
            ) {
                Text(if (numeroPregunta == totalPreguntas) "Finalizar Simulacro" else "Siguiente Pregunta")
            }
        }
    }
}