package com.umbral.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.umbral.app.ui.theme.Acento
import com.umbral.app.ui.theme.Tinta

/** Encabezado de pantalla sobre fondo tinta. */
@Composable
fun Encabezado(titulo: String, subtitulo: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Tinta, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
        if (subtitulo != null) {
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB4BDD6))
        }
    }
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

/** Tarjeta con número grande y etiqueta. */
@Composable
fun TarjetaDato(valor: String, etiqueta: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(valor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(etiqueta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Tarjeta contenedora estándar. */
@Composable
fun Tarjeta(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { contenido() }
    }
}

/** Barra con etiqueta y porcentaje. fraccion de 0f a 1f. */
@Composable
fun BarraProgreso(etiqueta: String, fraccion: Float, modifier: Modifier = Modifier, color: Color = Acento) {
    val f = fraccion.coerceIn(0f, 1f)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
            Text("${(f * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { f },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/** Etiqueta de estado (pendiente, asistió, etc.). */
@Composable
fun Etiqueta(texto: String, fondo: Color, textoColor: Color = Color.White, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = textoColor,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Estado vacío: dice qué pasa y qué hacer. */
@Composable
fun EstadoVacio(titulo: String, detalle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
