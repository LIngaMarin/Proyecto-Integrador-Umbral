package com.umbral.umbral_backend.dto

import com.umbral.umbral_backend.model.Dificultad

/** Datos que el controlador entrega al servicio para editar y aprobar una pregunta. */
data class ActualizarPreguntaDTO(
    /** Texto del enunciado actualizado. */
    val enunciado: String,
    /** Tema asignado a la pregunta. */
    val tema: String,
    /** Nivel de dificultad de la pregunta. */
    val dificultad: Dificultad,
    /** Opciones que se guardarán con la pregunta. */
    val opciones: List<String>,
    /** Texto de la opción correcta. */
    val respuestaCorrecta: String
)
