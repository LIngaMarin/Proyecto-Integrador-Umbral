package com.umbral.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.umbral.app.data.repository.AlumnoSeccionDao
import com.umbral.app.data.repository.AsistenciaDao
import com.umbral.app.data.repository.ClaseDocenteDao
import com.umbral.app.data.repository.ExplicacionDao
import com.umbral.app.data.repository.PreguntaPracticaDao
import com.umbral.app.data.repository.ProgresoDao
import com.umbral.app.data.repository.RespuestaTemporalDao
import com.umbral.app.data.repository.SimulacroDao

import com.umbral.app.data.repository.TemaDebilDao

@Database(
    entities = [
        AlumnoSeccionEntity::class,
        AsistenciaEntity::class,
        ClaseDocenteEntity::class,
        ExplicacionEntity::class,
        PreguntasPracticasEntity::class,
        ProgresoEntity::class,
        SimulacroResultadoEntity::class,
        RespuestaTemporalEntity::class,
        TemaDebilEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alumnoSeccionDao(): AlumnoSeccionDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun claseDocenteDao(): ClaseDocenteDao
    abstract fun explicacionDao(): ExplicacionDao
    abstract fun preguntaPracticaDao(): PreguntaPracticaDao
    abstract fun progresoDao(): ProgresoDao
    abstract fun respuestaTemporalDao(): RespuestaTemporalDao
    abstract fun simulacroDao(): SimulacroDao
    abstract fun temaDebilDao(): TemaDebilDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "umbral_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
