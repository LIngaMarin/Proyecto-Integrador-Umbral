package com.umbral.app.model

// Representa el temario que se trabajará durante una semana.
data class TemarioSemanal(
    val id: Int,
    val seccion: String,
    val semana: String,
    val temas: List<String>,
    val preguntas: List<Pregunta>
)