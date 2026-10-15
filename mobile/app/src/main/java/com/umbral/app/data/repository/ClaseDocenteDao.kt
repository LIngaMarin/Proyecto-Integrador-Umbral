package com.umbral.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbral.app.data.local.ClaseDocenteEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface ClaseDocenteDao {
    @Query("SELECT * FROM clases_docente WHERE diaSemana = :dia ORDER BY horaInicio ASC")
    fun obtenerClasesPorDia(dia: String): Flow<List<ClaseDocenteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarClases(clases: List<ClaseDocenteEntity>)
}