package com.umbral.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface AsistenciaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAsistencia(asistencia: AsistenciaEntity)

    @Query("SELECT * FROM asistencias ORDER BY id DESC")
    fun obtenerHistorialAsistencia(): Flow<List<AsistenciaEntity>>
}