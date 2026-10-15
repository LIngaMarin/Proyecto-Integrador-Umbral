package com.umbral.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "clases_docente")
data class ClaseDocenteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val curso: String,         // Ej: "Matemática I"
    val salon: String,         // Ej: "Aula 302"
    val horaInicio: String,    // Ej: "08:00 AM"
    val horaFin: String,       // Ej: "10:00 AM"
    val diaSemana: String      // Ej: "Lunes"
)
