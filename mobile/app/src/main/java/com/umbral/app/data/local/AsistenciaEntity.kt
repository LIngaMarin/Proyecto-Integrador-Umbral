package com.umbral.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asistencias")
data class AsistenciaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alumnoNombre: String,
    val sede: String,
    val fechahora: String,
    val estado: String,
    val qrValidoHasta: String
)