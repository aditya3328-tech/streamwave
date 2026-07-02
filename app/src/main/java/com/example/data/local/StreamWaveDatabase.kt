package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * StreamWave Database - Room configuration for all stream-related caching
 */
@Database(
    entities = [StreamCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StreamWaveDatabase : RoomDatabase() {
    abstract fun streamCacheDao(): StreamCacheDao
    
    companion object {
        @Volatile
        private var instance: StreamWaveDatabase? = null
        
        fun getInstance(context: Context): StreamWaveDatabase {
            return instance ?: synchronized(this) {
                instance ?: createDatabase(context).also { instance = it }
            }
        }
        
        private fun createDatabase(context: Context): StreamWaveDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                StreamWaveDatabase::class.java,
                "streamwave.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
