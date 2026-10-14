package com.umbral.app.model

// Representa una pregunta que el alumno puede resolver
data class Pregunta(
    val id: Int,
    val enunciado: String,
    val opciones: List<String>,
    val respuestaCorrecta: Int,
    val tema: String
)