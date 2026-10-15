package com.umbral.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.umbral.app.model.Pregunta
import com.umbral.app.ui.screens.practice.PracticaSemanalScreen
import com.umbral.app.ui.theme.UmbralTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UmbralTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Aplicamos el innerPadding que nos da el Scaffold mediante un modifier
                    PracticaSemanalScreen(
                        preguntas = preguntasEjemplo,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Preguntas temporales para comprobar el diseño.
// Luego las obtendremos desde el backend.
private val preguntasEjemplo = listOf(
    Pregunta(
        id = 1,
        enunciado = "¿Cuánto es 5 + 3?",
        opciones = listOf("6", "8", "10", "12"),
        respuestaCorrecta = 1,
        tema = "Aritmética"
    ),
    Pregunta(
        id = 2,
        enunciado = "¿Cuál es el resultado de 4 × 6?",
        opciones = listOf("10", "20", "24", "28"),
        respuestaCorrecta = 2,
        tema = "Multiplicación"
    ),
    Pregunta(
        id = 3,
        enunciado = "¿Cuánto es 15 - 7?",
        opciones = listOf("6", "7", "8", "9"),
        respuestaCorrecta = 2,
        tema = "Operaciones básicas"
    )
)

@androidx.compose.runtime.Composable
@Preview(showBackground = true)
fun PracticaSemanalPreview() {
    UmbralTheme {
        PracticaSemanalScreen(
            preguntas = preguntasEjemplo
        )
    }
}