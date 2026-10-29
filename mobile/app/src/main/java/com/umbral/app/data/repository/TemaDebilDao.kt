package com.umbral.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbral.app.data.local.TemaDebilEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemaDebilDao {
    @Query("SELECT * FROM temas_debiles WHERE simulacroId = :simulacroId ORDER BY porcentajeAcierto ASC")
    fun obtenerTemasDebilesPorSimulacro(simulacroId: Long): Flow<List<TemaDebilEntity>>

    @Query("SELECT * FROM temas_debiles ORDER BY porcentajeAcierto ASC")
    fun obtenerTodosLosTemasDebiles(): Flow<List<TemaDebilEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTemasDebiles(temas: List<TemaDebilEntity>)
}
