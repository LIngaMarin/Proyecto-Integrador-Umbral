package com.umbral.app.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.umbral.app.model.Pregunta

@Composable
fun PracticaSemanalScreen(
    preguntas: List<Pregunta>,
    modifier: Modifier = Modifier // 1. Añadimos el parámetro modifier con valor por defecto
) {
    Column(
        modifier = modifier // 2. Usamos el modifier que entra por parámetro
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Práctica de la semana",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Refuerza los temas vistos en clase.",
            style = MaterialTheme.typography.bodyLarge
        )

        if (preguntas.isEmpty()) {
            Text(
                text = "Todavía no hay preguntas disponibles."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = preguntas,
                    key = { pregunta -> pregunta.id }
                ) { pregunta ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = pregunta.tema,
                                style = MaterialTheme.typography.labelMedium
                            )

                            Text(
                                text = pregunta.enunciado,
                                style = MaterialTheme.typography.titleMedium
                            )

                            pregunta.opciones.forEachIndexed { indice, opcion ->
                                Row {
                                    Text(
                                        text = "${indice + 1}. $opcion"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}