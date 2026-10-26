package com.umbral.umbral_backend.dto

import com.umbral.umbral_backend.model.Dificultad

/** Contenido público de práctica; excluye la respuesta y los datos de revisión. */
data class PreguntaPracticaDTO(
    val id: Long,
    val enunciado: String,
    val tema: String,
    val dificultad: Dificultad,
    val opciones: List<String>
)
