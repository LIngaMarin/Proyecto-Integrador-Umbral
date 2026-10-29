package com.umbral.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temas_debiles")
data class TemaDebilEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val simulacroId: Long,
    val tema: String,
    val porcentajeAcierto: Float,
    val preguntasIncorrectas: Int,
    val preguntasTotales: Int,
    val recomendacion: String
)
