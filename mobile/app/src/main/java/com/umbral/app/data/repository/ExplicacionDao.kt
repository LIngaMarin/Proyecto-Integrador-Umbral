package com.umbral.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbral.app.data.local.ExplicacionEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface ExplicacionDao {
    @Query("SELECT * FROM explicaciones_ia WHERE preguntaId = :preguntaId LIMIT 1")
    fun obtenerExplicacionPorPregunta(preguntaId: Long): Flow<ExplicacionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarExplicacion(explicacion: ExplicacionEntity)
}