package com.umbral.app.ui.screens.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umbral.app.model.Asistencia

@Composable
fun AsistenciaQrScreen(
    asistencia: Asistencia,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Registro de Asistencia QR",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Confirmación de presencia en sede para el apoderado.",
            style = MaterialTheme.typography.bodyLarge
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Alumno: ${asistencia.alumnoNombre}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Sede: ${asistencia.sede}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Fecha y Hora: ${asistencia.fechaHora}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Estado: ${asistencia.estado}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}