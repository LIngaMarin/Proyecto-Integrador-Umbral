package com.umbral.app.ui.screens.attendance

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.umbral.app.model.Asistencia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciaQrScreen(
    asistencia: Asistencia,
    modifier: Modifier = Modifier
) {
    var qrCodeInput by remember { mutableStateOf("QR-UMBRAL-2026-CONFIRMED") }
    var estaValidando by remember { mutableStateOf(false) }
    var asistenciaConfirmada by remember { mutableStateOf<Asistencia?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmación de Asistencia por QR") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📷 Escáner de Código QR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Escanea el código QR proyectado en clase o ingresa el código de validación.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedTextField(
                value = qrCodeInput,
                onValueChange = { qrCodeInput = it },
                label = { Text("Código Token del QR") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    estaValidando = true
                    // Simulación de envío a endpoint backend de validación QR
                    asistenciaConfirmada = asistencia.copy(
                        estado = "Puntual (Confirmado por QR)",
                        fechaHora = "Hoy, 08:30 AM"
                    )
                    estaValidando = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = qrCodeInput.isNotEmpty() && !estaValidando
            ) {
                if (estaValidando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Validando QR...")
                } else {
                    Text("✅ Validar Asistencia QR")
                }
            }

            val actualConfirmada = asistenciaConfirmada
            if (actualConfirmada != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "¡Asistencia Confirmada Exitosamente!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "Alumno: ${actualConfirmada.alumnoNombre}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Sede: ${actualConfirmada.sede}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Fecha y Hora: ${actualConfirmada.fechaHora}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Estado: ${actualConfirmada.estado}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Último registro de asistencia",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
    }
}
