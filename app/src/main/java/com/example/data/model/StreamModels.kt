package com.example.data.model

import com.google.gson.annotations.SerializedName

/**
 * Stream Model - Normalized stream data from any provider
 * Mirrors web's stream normalization structure
 */
data class Stream(
    val id: String,
    val provider: String,
    val url: String,
    val type: String, // "hls" or "mp4"
    val quality: String,
    val resolutions: String? = null,
    val size: Long? = null,
    val duration: Long? = null,
    val codecName: String? = null,
    val numericQuality: Int = 0,
    val contentType: String? = null
)

/**
 * Caption/Subtitle Model
 */
data class Caption(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("url")
    val url: String,
    @SerializedName("lan")
    val language: String = "en",
    @SerializedName("lanName")
    val languageName: String = "English"
)

/**
 * Provider Stream Response - Structured response from any provider
 */
data class ProviderStreamResponse(
    val provider: String,
    val success: Boolean,
    val streams: List<Stream> = emptyList(),
    val message: String,
    val error: String? = null
)

/**
 * Resolved Stream Payload - Final result from stream resolver
 */
data class ResolvedStreamPayload(
    val success: Boolean,
    val streams: List<Stream> = emptyList(),
    val selectedStream: Stream? = null,
    val attempts: List<ProviderStreamResponse> = emptyList(),
    val requested: StreamRequest,
    val fromCache: Boolean = false
)

/**
 * Stream Request Parameters
 */
data class StreamRequest(
    val subjectId: String,
    val detailPath: String = "",
    val se: Int = 0,
    val ep: Int = 0
)
