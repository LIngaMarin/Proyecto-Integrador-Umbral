package com.umbral.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.stream.IntStream

@Entity(tableName = "progreso_semanal")
data class ProgresoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val semanas: String,
    val curso: String,
    val practicasCompletadas: Int,
    val practicasTotales: Int,
    val estado: String
)
