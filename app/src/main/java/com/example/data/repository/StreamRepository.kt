package com.example.data.repository

import android.content.Context
import com.example.data.local.StreamCacheManager
import com.example.data.local.StreamWaveDatabase
import com.example.data.model.Caption
import com.example.data.model.ResolvedStreamPayload
import com.example.data.model.StreamRequest
import com.example.data.remote.StreamResolver
import com.example.data.remote.providers.MovieBoxProvider

/**
 * Stream Repository - High-level API for all streaming operations
 * Orchestrates HTTP clients, caching, validation, and provider selection
 *
 * Mirrors web architecture:
 * - axios.js → ApiClient
 * - moviebox.js → MovieBoxProvider
 * - validators.js → StreamValidator
 * - streamResolver.js → StreamResolver
 * - cache.js → StreamCacheManager + Room Database
 */
class StreamRepository(context: Context) {

    private val db = StreamWaveDatabase.getInstance(context)
    private val cacheManager = StreamCacheManager(db.streamCacheDao())
    private val streamResolver = StreamResolver(cacheManager)

    /**
     * Main entry point: Resolve streams for a subject
     * Returns streams from any available provider with fallback logic
     */
    suspend fun getStreams(
        subjectId: String,
        detailPath: String = "",
        season: Int = 0,
        episode: Int = 0
    ): ResolvedStreamPayload {
        return streamResolver.resolveStreams(
            StreamRequest(
                subjectId = subjectId,
                detailPath = detailPath,
                se = season,
                ep = episode
            )
        )
    }

    /**
     * Get captions for specific content
     */
    suspend fun getCaptions(
        id: String,
        subjectId: String,
        detailPath: String = ""
    ): List<Caption> {
        return try {
            MovieBoxProvider.getCaptions(id, subjectId, detailPath)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clear all cached streams
     */
    suspend fun clearCache() {
        cacheManager.clearCache()
    }
    
    /**
     * Clear expired cache entries
     */
    suspend fun clearExpiredCache() {
        cacheManager.clearExpiredCache()
    }
}
