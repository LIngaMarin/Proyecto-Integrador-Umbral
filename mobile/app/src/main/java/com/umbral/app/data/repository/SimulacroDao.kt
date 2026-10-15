package com.umbral.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbral.app.data.local.SimulacroResultadoEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface SimulacroDao {
    @Query("SELECT * FROM simulacro_resultados ORDER BY id DESC")
    fun obtenerHistorialSimulacros(): Flow<List<SimulacroResultadoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarResultado(resultado: SimulacroResultadoEntity)
}