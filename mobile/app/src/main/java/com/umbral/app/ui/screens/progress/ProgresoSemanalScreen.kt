package com.umbral.app.ui.screens.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.umbral.app.data.local.ProgresoEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgresoSemanalScreen(
    listaProgreso: List<ProgresoEntity>,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progreso Semanal de Práctica") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listaProgreso) { progreso ->
                ProgresoCard(progreso = progreso)
            }
        }
    }
}

@Composable
fun ProgresoCard(progreso: ProgresoEntity) {
    val porcentaje = if (progreso.practicasTotales > 0) {
        progreso.practicasCompletadas.toFloat() / progreso.practicasTotales.toFloat()
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progreso.semanas,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Badge(
                    containerColor = if (progreso.estado == "Completado") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                ) {
                    Text(text = progreso.estado, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = progreso.curso,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { porcentaje },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Completadas: ${progreso.practicasCompletadas} de ${progreso.practicasTotales}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}