package com.umbral.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "respuestas_temporales")
data class RespuestaTemporalEntity(
    @PrimaryKey
    val preguntaId: Long,
    val simulacroId: Long,
    val respuestaSeleccionada: String,
    val fechaGuardado: Long = System.currentTimeMillis()
)
