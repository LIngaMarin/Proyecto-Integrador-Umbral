package com.umbral.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preguntas_practicas")
data class PreguntasPracticasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tema: String,
    val enunciado: String,
    val opcionA: String,
    val opcionB: String,
    val opcionC: String,
    val opcionD: String,
    val respuestaCorrecta: String
)
