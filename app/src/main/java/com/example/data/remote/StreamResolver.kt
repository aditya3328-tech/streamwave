package com.example.data.remote

import android.util.Log
import com.example.data.local.StreamCacheManager
import com.example.data.model.ProviderStreamResponse
import com.example.data.model.ResolvedStreamPayload
import com.example.data.model.Stream
import com.example.data.model.StreamRequest
import com.example.data.remote.providers.ProviderRegistry
import com.google.gson.Gson
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Stream Resolver - Core orchestration for multi-provider stream resolution
 * Mirrors web's streamResolver.js with fallback logic, validation, caching, and quality selection
 */
class StreamResolver(
    private val cacheManager: StreamCacheManager?,
    private val validator: StreamValidator = StreamValidator
) {
    private val gson = Gson()
    private val TAG = "StreamResolver"
    
    /**
     * Resolve streams with multi-provider fallback strategy
     */
    suspend fun resolveStreams(request: StreamRequest): ResolvedStreamPayload = coroutineScope {
        try {
            ApiClient.log(
                "info",
                "Starting stream resolution",
                "subjectId=${request.subjectId}, season=${request.se}, episode=${request.ep}"
            )
            
            val cacheKey = makeCacheKey(request)
            
            // Check cache first
            cacheManager?.getCachedStreams(cacheKey)?.let { cached ->
                ApiClient.log("info", "Cache hit for streams", "cacheKey=$cacheKey")
                return@coroutineScope ResolvedStreamPayload(
                    success = true,
                    streams = cached.streams,
                    selectedStream = cached.selectedStream,
                    attempts = emptyList(),
                    requested = request,
                    fromCache = true
                )
            }
            
            val attempts = mutableListOf<ProviderStreamResponse>()
            var selectedStream: Stream? = null
            var validStreams = listOf<Stream>()
            
            // Try each provider in sequence
            for (provider in ProviderRegistry.providers) {
                try {
                    val response = provider.getStreams(
                        subjectId = request.subjectId,
                        detailPath = request.detailPath,
                        season = request.se,
                        episode = request.ep
                    )
                    attempts.add(response)
                    
                    if (!response.success || response.streams.isEmpty()) {
                        ApiClient.log("warn", "Provider returned no streams", 
                            "provider=${response.provider}")
                        continue
                    }
                    
                    // FLOW_TRACE LOG 1 — provider parsed streams BEFORE validation gate
                    Log.e("FLOW_TRACE", "Parsed stream count = ${response.streams.size}")
                    Log.e("FLOW_TRACE", "Parsed streams = ${response.streams.map { it.quality to it.url }}")

                    // Validate streams in parallel
                    val validationTasks = response.streams.map { stream ->
                        async {
                            val validation = validator.validateStreamUrl(stream.url)
                            Pair(stream, validation)
                        }
                    }

                    val validationResults = validationTasks.awaitAll()
                    validStreams = validationResults
                        .filter { (_, validation) -> validation.valid }
                        .map { (stream, validation) ->
                            stream.copy(contentType = validation.contentType)
                        }

                    // FLOW_TRACE LOG 2 — how many survived the HEAD validation probe
                    Log.e("FLOW_TRACE", "Validated stream count = ${validStreams.size} (of ${response.streams.size})")

                    // ROOT-CAUSE FIX (parity with website): validateStreamUrl() fires a header-less
                    // HEAD probe at the CDN (bcdnxw.hakunaymatata.com), which is Referer-locked and
                    // answers 403 to any request without a movie-box.co Referer. That marks EVERY
                    // valid stream invalid, empties validStreams, and surfaces as the bogus
                    // "No playable stream available" error — even though the URLs play fine once
                    // ExoPlayer sends the Referer (see PlayerScreen MediaPlaybackHttpClient setup).
                    // The website performs NO such pre-validation. So when validation drops every
                    // stream, fall back to the raw provider streams instead of failing playback.
                    if (validStreams.isEmpty() && response.streams.isNotEmpty()) {
                        Log.e("FLOW_TRACE", "All ${response.streams.size} streams failed HEAD validation " +
                            "(Referer-locked CDN) — falling back to UNVALIDATED provider streams")
                        validStreams = response.streams
                    }

                    if (validStreams.isNotEmpty()) {
                        selectedStream = selectBestStream(validStreams)
                        ApiClient.log("info", "Valid streams found", 
                            "provider=${response.provider}, count=${validStreams.size}")
                        break // Stop at first successful provider
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Provider error: ${e.message}", e)
                    ApiClient.log("error", "Provider exception", 
                        "provider=unknown, error=${e.message}")
                }
            }
            
            if (validStreams.isNotEmpty() && selectedStream != null) {
                // Cache successful result
                cacheManager?.cacheStreams(
                    cacheKey = cacheKey,
                    streams = validStreams,
                    selectedStream = selectedStream,
                    providerName = selectedStream.provider,
                    requestJson = gson.toJson(request)
                )
            }
            
            // FLOW_TRACE LOG 3 — final resolver output handed back to the repository
            Log.e("FLOW_TRACE", "Resolver returning count = ${validStreams.size}, " +
                "success = ${validStreams.isNotEmpty()}, selectedUrl = ${selectedStream?.url}")

            ResolvedStreamPayload(
                success = validStreams.isNotEmpty(),
                streams = validStreams,
                selectedStream = selectedStream,
                attempts = attempts,
                requested = request,
                fromCache = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Stream resolution failed: ${e.message}", e)
            ResolvedStreamPayload(
                success = false,
                streams = emptyList(),
                selectedStream = null,
                attempts = emptyList(),
                requested = request,
                fromCache = false
            )
        }
    }
    
    /**
     * Select best stream based on quality and type preference
     * Algorithm: Sort by numeric quality DESC, HLS > MP4, then first valid
     */
    private fun selectBestStream(streams: List<Stream>): Stream? {
        if (streams.isEmpty()) return null
        
        return streams.sortedWith(
            compareBy<Stream> { it.numericQuality }
                .thenBy { if (it.type == "hls") 0 else 1 } // HLS first
                .reversed() // Highest quality first
        ).firstOrNull()
    }
    
    /**
     * Generate cache key from request parameters
     */
    private fun makeCacheKey(request: StreamRequest): String {
        return "stream_${request.subjectId}_s${request.se}_e${request.ep}"
    }
}
