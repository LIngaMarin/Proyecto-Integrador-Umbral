package com.umbral.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "explicaciones_ia")
data class ExplicacionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val preguntaId: Long,
    val respuestaAlumno: String,
    val respuestaCorrecta: String,
    val explicacionIa: String,
    val fechaConsulta: String
)
