package com.example.data.remote

import android.util.Log
import com.example.data.model.Stream
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Stream Validators & Normalizers
 * Mirrors web's validators.js functionality
 */
object StreamValidator {
    private const val TAG = "StreamValidator"
    
    private val TYPE_CHECK = mapOf(
        "hls" to listOf(
            "application/vnd.apple.mpegurl",
            "application/x-mpegURL",
            "video/MP2T"
        ),
        "mp4" to listOf(
            "video/mp4",
            "application/octet-stream",
            "audio/mp4"
        )
    )
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
    
    /**
     * Detect stream type from URL
     */
    fun detectStreamType(url: String?): String {
        if (url == null || url.isEmpty()) return "unknown"
        
        val normalized = url.lowercase()
        return when {
            normalized.contains(".m3u8") -> "hls"
            normalized.contains(".mp4") -> "mp4"
            else -> "unknown"
        }
    }
    
    /**
     * Parse numeric quality from string
     */
    fun parseQuality(label: String?): Int {
        if (label == null) return 0
        
        // Match pattern like "1080p", "720p", etc.
        val regex = Regex("""(\d{3,4})p""", RegexOption.IGNORE_CASE)
        val match = regex.find(label)
        if (match != null) {
            return match.groupValues[1].toIntOrNull() ?: 0
        }
        
        // Try parsing as direct number
        return label.toIntOrNull() ?: 0
    }
    
    /**
     * Normalize stream from provider response
     */
    fun normalizeStream(
        item: Any?,
        provider: String,
        defaultQuality: String = "Unknown"
    ): Stream? {
        if (item == null) return null
        
        val url = when (item) {
            is String -> item
            is Map<*, *> -> {
                item["url"] as? String
                    ?: item["streamUrl"] as? String
                    ?: item["playUrl"] as? String
                    ?: item["video"] as? String
                    ?: return null
            }
            else -> return null
        }
        
        if (url.isEmpty()) return null
        
        val type = detectStreamType(url)
        if (type == "unknown") return null
        
        val quality = when (item) {
            is Map<*, *> -> {
                item["resolutions"] as? String
                    ?: item["resolution"] as? String
                    ?: item["quality"] as? String
                    ?: item["label"] as? String
                    ?: defaultQuality
            }
            else -> defaultQuality
        }
        
        val numericQuality = parseQuality(quality)
        
        return Stream(
            id = when (item) {
                is Map<*, *> -> (item["id"] as? String
                    ?: item["streamId"] as? String
                    ?: url)
                else -> url
            },
            provider = provider,
            url = url,
            type = type,
            quality = quality,
            numericQuality = numericQuality,
            resolutions = quality
        )
    }
    
    /**
     * Validate stream URL by checking HEAD request
     */
    suspend fun validateStreamUrl(url: String): ValidationResult {
        if (url.isEmpty()) {
            return ValidationResult(valid = false, contentType = null, type = "unknown")
        }
        
        val type = detectStreamType(url)
        if (type == "unknown") {
            return ValidationResult(valid = false, contentType = null, type = type)
        }
        
        return try {
            val request = Request.Builder()
                .head()
                .url(url)
                .header("User-Agent", "StreamWave/1.0")
                .build()
            
            val response = httpClient.newCall(request).execute()
            val contentType = response.header("content-type")
                ?.split(";")
                ?.firstOrNull()
                ?.trim()
            
            val valid = contentType != null && 
                TYPE_CHECK[type]?.any { allowed ->
                    contentType.lowercase().contains(allowed.lowercase())
                } == true
            
            ValidationResult(
                valid = valid,
                contentType = contentType,
                type = type
            )
        } catch (e: Exception) {
            Log.e(TAG, "URL validation failed: ${e.message}")
            ValidationResult(valid = false, contentType = null, type = type)
        }
    }
    
    data class ValidationResult(
        val valid: Boolean,
        val contentType: String?,
        val type: String
    )
}
