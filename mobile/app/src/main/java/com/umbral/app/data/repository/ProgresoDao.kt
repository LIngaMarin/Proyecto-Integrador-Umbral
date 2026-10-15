package com.umbral.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbral.app.data.local.ProgresoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgresoDao {
    @Query("SELECT * FROM progreso_semanal")
    fun obtenerTodoElProgreso(): Flow<List<ProgresoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarProgreso(progreso: ProgresoEntity)
}