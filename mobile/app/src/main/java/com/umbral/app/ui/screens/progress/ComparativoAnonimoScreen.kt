package com.umbral.app.ui.screens.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ComparativoDesempenoDto(
    val miPuntajePromedio: Float,
    val promedioSeccion: Float,
    val mejorPuntajeSeccion: Float,
    val percentilAlumno: Int,
    val totalAlumnosAnonimos: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparativoAnonimoScreen(
    datosComparativos: ComparativoDesempenoDto,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparativo Anónimo de Desempeño") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔒 Privacidad y Anonimato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Comparas tu rendimiento promedio frente al conjunto de ${datosComparativos.totalAlumnosAnonimos} alumnos de tu sección de forma 100% anónima.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Gráfico Comparativo Canvas Custom
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📊 Comparativa de Promedios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    GraficoBarrasComparativo(
                        miPuntaje = datosComparativos.miPuntajePromedio,
                        promedioSeccion = datosComparativos.promedioSeccion,
                        mejorPuntaje = datosComparativos.mejorPuntajeSeccion
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LeyendaItem(color = MaterialTheme.colorScheme.primary, etiqueta = "Mi Nota (${datosComparativos.miPuntajePromedio})")
                        LeyendaItem(color = MaterialTheme.colorScheme.secondary, etiqueta = "Promedio (${datosComparativos.promedioSeccion})")
                        LeyendaItem(color = MaterialTheme.colorScheme.tertiary, etiqueta = "Máximo (${datosComparativos.mejorPuntajeSeccion})")
                    }
                }
            }

            // Tarjeta de Percentil
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🏆 Posicionamiento Percentil",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Superas al ${datosComparativos.percentilAlumno}% de los alumnos de tu sección.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun GraficoBarrasComparativo(
    miPuntaje: Float,
    promedioSeccion: Float,
    mejorPuntaje: Float
) {
    var animado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animado = true }

    val factorMiNota by animateFloatAsState(targetValue = if (animado) miPuntaje / 20f else 0f, animationSpec = tween(1000), label = "miNota")
    val factorPromedio by animateFloatAsState(targetValue = if (animado) promedioSeccion / 20f else 0f, animationSpec = tween(1000), label = "promedio")
    val factorMejor by animateFloatAsState(targetValue = if (animado) mejorPuntaje / 20f else 0f, animationSpec = tween(1000), label = "mejor")

    val colorMiNota = MaterialTheme.colorScheme.primary
    val colorPromedio = MaterialTheme.colorScheme.secondary
    val colorMejor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 24.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val anchoBarra = 40.dp.toPx()
            val espacio = (canvasWidth - (anchoBarra * 3)) / 4

            val alturaMax = canvasHeight - 30.dp.toPx()

            // Dibuja líneas de guía (0, 10, 20)
            val y20 = 10.dp.toPx()
            val y10 = y20 + (alturaMax / 2)
            val y0 = y20 + alturaMax

            drawLine(Color.LightGray, Offset(0f, y20), Offset(canvasWidth, y20), strokeWidth = 2f)
            drawLine(Color.LightGray, Offset(0f, y10), Offset(canvasWidth, y10), strokeWidth = 2f)
            drawLine(Color.Gray, Offset(0f, y0), Offset(canvasWidth, y0), strokeWidth = 4f)

            // Barra 1: Mi Nota
            val x1 = espacio
            val h1 = alturaMax * factorMiNota
            drawRoundRect(
                color = colorMiNota,
                topLeft = Offset(x1, y0 - h1),
                size = Size(anchoBarra, h1),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Barra 2: Promedio Sección
            val x2 = espacio * 2 + anchoBarra
            val h2 = alturaMax * factorPromedio
            drawRoundRect(
                color = colorPromedio,
                topLeft = Offset(x2, y0 - h2),
                size = Size(anchoBarra, h2),
                cornerRadius = CornerRadius(12f, 12f)
            )

            // Barra 3: Mejor Nota
            val x3 = espacio * 3 + anchoBarra * 2
            val h3 = alturaMax * factorMejor
            drawRoundRect(
                color = colorMejor,
                topLeft = Offset(x3, y0 - h3),
                size = Size(anchoBarra, h3),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
    }
}

@Composable
fun LeyendaItem(color: Color, etiqueta: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.height(12.dp).width(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = etiqueta, style = MaterialTheme.typography.labelMedium)
    }
}
