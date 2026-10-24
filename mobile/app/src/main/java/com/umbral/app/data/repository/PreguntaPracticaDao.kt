package com.umbral.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbral.app.data.local.PreguntasPracticasEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface PreguntaPracticaDao {
    @Query("SELECT DISTINCT tema FROM preguntas_practicas")
    fun obtenerTemasDisponibles(): Flow<List<String>>

    @Query("SELECT * FROM preguntas_practicas WHERE tema = :temaElegido")
    fun obtenerPreguntasPorTema(temaElegido: String): Flow<List<PreguntasPracticasEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarPregunta(pregunta: PreguntasPracticasEntity)
}