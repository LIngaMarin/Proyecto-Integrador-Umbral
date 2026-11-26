package com.umbral.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.umbral.app.data.local.cache.CacheDao
import com.umbral.app.data.local.cache.ClaseCacheEntity
import com.umbral.app.data.local.cache.MarcaEntity
import com.umbral.app.data.local.cache.PreguntaCacheEntity
import com.umbral.app.data.local.cache.SemanaCacheEntity

/**
 * Base de datos local (Room / SQLite) del celular. Solo es una cache para el modo offline:
 * la base de datos real del sistema es PostgreSQL, detras de Spring Boot.
 */
@Database(
    entities = [ClaseCacheEntity::class, PreguntaCacheEntity::class, SemanaCacheEntity::class, MarcaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class UmbralDatabase : RoomDatabase() {

    abstract fun cacheDao(): CacheDao

    companion object {
        fun crear(context: Context): UmbralDatabase =
            Room.databaseBuilder(context.applicationContext, UmbralDatabase::class.java, "umbral.db")
                .fallbackToDestructiveMigration(dropAllTables = true) // es cache: si cambia el esquema se rehace
                .build()
    }
}
