package com.umbral.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "alumnos_seccion")
data class AlumnoSeccionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val seccionId: String,        // Ej: "SEC-A1"
    val nombreCompleto: String,
    val codigoAlumno: String,
    val fotoUrl: String? = null
)
