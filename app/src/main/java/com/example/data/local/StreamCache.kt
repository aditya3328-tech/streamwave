package com.example.data.local

import androidx.room.*
import com.google.gson.Gson
import com.example.data.model.Stream

/**
 * Stream Cache Entity - Room Database model
 * Stores resolved streams with TTL support
 */
@Entity(tableName = "stream_cache")
data class StreamCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    
    @ColumnInfo(name = "streams_json")
    val streamsJson: String,
    
    @ColumnInfo(name = "selected_stream_json")
    val selectedStreamJson: String?,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "request_json")
    val requestJson: String,
    
    @ColumnInfo(name = "provider_name")
    val providerName: String
) {
    companion object {
        const val CACHE_TTL_MS = 3600000L // 1 hour
        
        fun isExpired(entity: StreamCacheEntity): Boolean {
            return (System.currentTimeMillis() - entity.timestamp) > CACHE_TTL_MS
        }
    }
}

/**
 * Stream Cache DAO - Database access object
 */
@Dao
interface StreamCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StreamCacheEntity)
    
    @Query("SELECT * FROM stream_cache WHERE cacheKey = :key")
    suspend fun getByKey(key: String): StreamCacheEntity?
    
    @Query("DELETE FROM stream_cache WHERE timestamp < :cutoffTime")
    suspend fun deleteExpired(cutoffTime: Long)
    
    @Query("DELETE FROM stream_cache")
    suspend fun clearAll()
}

/**
 * Stream Cache Manager - High-level cache operations
 */
class StreamCacheManager(private val dao: StreamCacheDao) {
    private val gson = Gson()
    
    suspend fun getCachedStreams(cacheKey: String): CacheResult? {
        val entity = dao.getByKey(cacheKey)
        
        if (entity != null && !StreamCacheEntity.isExpired(entity)) {
            return try {
                CacheResult(
                    streams = gson.fromJson(entity.streamsJson, Array<Stream>::class.java).toList(),
                    selectedStream = entity.selectedStreamJson?.let { 
                        gson.fromJson(it, Stream::class.java) 
                    },
                    providerName = entity.providerName,
                    timestamp = entity.timestamp
                )
            } catch (e: Exception) {
                null
            }
        }
        
        // Delete if expired
        if (entity != null && StreamCacheEntity.isExpired(entity)) {
            dao.deleteExpired(System.currentTimeMillis())
        }
        
        return null
    }
    
    suspend fun cacheStreams(
        cacheKey: String,
        streams: List<Stream>,
        selectedStream: Stream?,
        providerName: String,
        requestJson: String
    ) {
        val entity = StreamCacheEntity(
            cacheKey = cacheKey,
            streamsJson = gson.toJson(streams),
            selectedStreamJson = selectedStream?.let { gson.toJson(it) },
            timestamp = System.currentTimeMillis(),
            requestJson = requestJson,
            providerName = providerName
        )
        dao.insert(entity)
    }
    
    suspend fun clearCache() {
        dao.clearAll()
    }
    
    suspend fun clearExpiredCache() {
        dao.deleteExpired(System.currentTimeMillis())
    }
    
    data class CacheResult(
        val streams: List<Stream>,
        val selectedStream: Stream?,
        val providerName: String,
        val timestamp: Long
    )
}
