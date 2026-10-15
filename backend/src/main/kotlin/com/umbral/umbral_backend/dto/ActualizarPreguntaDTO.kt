package com.umbral.umbral_backend.dto

import com.umbral.umbral_backend.model.Dificultad

data class ActualizarPreguntaDTO(
    val enunciado: String,
    val tema: String,
    val dificultad: Dificultad,
    val opciones: List<String>,
    val respuestaCorrecta: String
)