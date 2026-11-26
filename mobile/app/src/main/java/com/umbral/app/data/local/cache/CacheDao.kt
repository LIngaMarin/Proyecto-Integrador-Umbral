package com.umbral.app.data.local.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class CacheDao {

    // ---- Horario ----
    @Query("SELECT * FROM cache_clases ORDER BY dia, horaInicio")
    abstract suspend fun clases(): List<ClaseCacheEntity>

    @Query("DELETE FROM cache_clases")
    abstract suspend fun borrarClases()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarClases(clases: List<ClaseCacheEntity>)

    /** Borra y vuelve a insertar en una sola transaccion. */
    @Transaction
    open suspend fun reemplazarClases(clases: List<ClaseCacheEntity>) {
        borrarClases()
        insertarClases(clases)
    }

    // ---- Preguntas ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarPreguntas(preguntas: List<PreguntaCacheEntity>)

    @Query("SELECT * FROM cache_preguntas WHERE tema = :tema ORDER BY id")
    abstract suspend fun preguntasPorTema(tema: String): List<PreguntaCacheEntity>

    @Query("SELECT DISTINCT tema FROM cache_preguntas ORDER BY tema")
    abstract suspend fun temas(): List<String>

    @Query(
        "SELECT p.* FROM cache_preguntas p " +
            "INNER JOIN cache_semana s ON s.preguntaId = p.id ORDER BY s.orden"
    )
    abstract suspend fun preguntasSemana(): List<PreguntaCacheEntity>

    @Query("DELETE FROM cache_semana")
    abstract suspend fun borrarSemana()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarSemana(semana: List<SemanaCacheEntity>)

    @Transaction
    open suspend fun reemplazarSemana(preguntas: List<PreguntaCacheEntity>, semana: List<SemanaCacheEntity>) {
        insertarPreguntas(preguntas)
        borrarSemana()
        insertarSemana(semana)
    }

    // ---- Marcas del alumno ----
    @Query("SELECT * FROM marcas_practica")
    abstract suspend fun marcas(): List<MarcaEntity>

    @Query("SELECT * FROM marcas_practica WHERE preguntaId = :preguntaId")
    abstract suspend fun marca(preguntaId: Int): MarcaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun guardarMarca(marca: MarcaEntity)
}
