package com.umbral.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbral.app.data.local.AlumnoSeccionEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface AlumnoSeccionDao {
    @Query("SELECT * FROM alumnos_seccion WHERE seccionId = :seccionId ORDER BY nombreCompleto ASC")
    fun obtererAlumnosPorSeccion(seccionId: String): Flow<List<AlumnoSeccionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarAlumnos(alumnos: List<AlumnoSeccionEntity>)
}