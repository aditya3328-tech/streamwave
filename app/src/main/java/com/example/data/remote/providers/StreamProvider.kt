package com.example.data.remote.providers

import com.example.data.model.Caption
import com.example.data.model.ProviderStreamResponse

/**
 * Provider Interface - Standardized contract for all stream providers
 */
interface StreamProvider {
    suspend fun getStreams(
        subjectId: String,
        detailPath: String = "",
        season: Int = 0,
        episode: Int = 0
    ): ProviderStreamResponse
    
    suspend fun getCaptions(
        id: String,
        subjectId: String,
        detailPath: String
    ): List<Caption>
}

/**
 * Stream Provider Factory - Registry of available providers
 */
object ProviderRegistry {
    val providers: List<StreamProvider> = listOf(
        MovieBoxProviderImpl
    )

    fun getProvider(name: String): StreamProvider? {
        return when (name) {
            "moviebox" -> MovieBoxProviderImpl
            else -> null
        }
    }
}

/**
 * MovieBox Provider Implementation
 */
object MovieBoxProviderImpl : StreamProvider {
    override suspend fun getStreams(
        subjectId: String,
        detailPath: String,
        season: Int,
        episode: Int
    ): ProviderStreamResponse {
        return MovieBoxProvider.getStreams(subjectId, detailPath, season, episode)
    }
    
    override suspend fun getCaptions(
        id: String,
        subjectId: String,
        detailPath: String
    ): List<Caption> {
        return MovieBoxProvider.getCaptions(id, subjectId, detailPath)
    }
}
