package com.umbral.app.data.local.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

// Tablas de Room que guardan una copia de lo que llego de la API, para usar la app sin internet.

@Entity(tableName = "cache_clases")
data class ClaseCacheEntity(
    @PrimaryKey val id: Int,
    val curso: String,
    val seccion: String,
    val aula: String,
    val sede: String,
    val dia: Int,
    val horaInicio: String,
    val horaFin: String,
    val docente: String,
)

/** opciones y solucionario se guardan como un solo texto separado (ver Mapeos.kt). */
@Entity(tableName = "cache_preguntas")
data class PreguntaCacheEntity(
    @PrimaryKey val id: Int,
    val tema: String,
    val enunciado: String,
    val opciones: String,
    val correcta: Int,
    val solucionario: String,
    val dificultad: String,
)

/** Que preguntas forman la practica de la semana y en que orden. */
@Entity(tableName = "cache_semana")
data class SemanaCacheEntity(
    @PrimaryKey val orden: Int,
    val preguntaId: Int,
)

/** Dificultad y favorita que marco el alumno (se guardan aunque no haya internet). */
@Entity(tableName = "marcas_practica")
data class MarcaEntity(
    @PrimaryKey val preguntaId: Int,
    val dificultad: String?,
    val favorita: Boolean,
)
