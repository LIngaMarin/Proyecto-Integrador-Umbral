package com.umbral.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbral.app.data.local.RespuestaTemporalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RespuestaTemporalDao {
    @Query("SELECT * FROM respuestas_temporales WHERE simulacroId = :simulacroId")
    fun obtenerRespuestasPorSimulacro(simulacroId: Long): Flow<List<RespuestaTemporalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarRespuestaTemporal(respuesta: RespuestaTemporalEntity)

    @Query("DELETE FROM respuestas_temporales WHERE simulacroId = :simulacroId")
    suspend fun limpiarRespuestasTemporales(simulacroId: Long)
}
