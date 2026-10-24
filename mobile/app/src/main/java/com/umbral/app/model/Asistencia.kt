package com.umbral.app.model

data class Asistencia (
    val id: Long,
    val alumnoNombre: String,
    val sede: String,
    val fechaHora: String,
    val estado: String, // EJ: Puntual, Tardanza, Falta
    val qrValidoHasta: String
    )