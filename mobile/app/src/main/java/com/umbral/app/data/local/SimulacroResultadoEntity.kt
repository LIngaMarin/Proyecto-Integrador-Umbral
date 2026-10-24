package com.umbral.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "simulacro_resultados")
data class SimulacroResultadoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tituloSimulacro: String,   // Ej: "Simulacro General Admisión 2026"
    val puntajeObtenido: Float,
    val respuestasCorrectas: Int,
    val respuestasIncorrectas: Int,
    val respuestasEnBlanco: Int,
    val fechaRealizacion: String
)
