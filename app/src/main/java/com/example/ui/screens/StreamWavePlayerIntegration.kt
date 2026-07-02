package com.example.ui.screens

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.data.model.PlaybackQuality
import com.example.data.model.StreamPlayback
import com.example.data.model.SubtitleTrack
import com.example.data.repository.StreamRepository
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.PlayerViewModelFactory
import androidx.compose.ui.platform.LocalContext

/**
 * StreamWave Playback Integration Helper
 * Bridges StreamWave streaming architecture with existing player screen
 * 
 * Usage:
 * val streamPlayback = resolveStreamPlayback(context, "subject-123", 1, 5)
 * // Returns ready-to-play StreamPlayback with resolved streams
 */

/**
 * Resolve streams using StreamWave architecture
 * Returns playback data compatible with existing player
 */
suspend fun resolveStreamPlayback(
    context: android.content.Context,
    subjectId: String,
    season: Int = 0,
    episode: Int = 0
): StreamPlayback? {
    return try {
        val repository = StreamRepository(context)
        val result = repository.getStreams(
            subjectId = subjectId,
            season = season,
            episode = episode
        )
        
        val stream = result.selectedStream
        if (result.success && stream != null) {
            Log.d("StreamWave", "✅ Resolved stream: ${stream.quality} ${stream.type}")

            // Get captions
            val captions = repository.getCaptions(
                id = stream.id,
                subjectId = subjectId
            )

            // Create StreamPlayback from StreamWave result.
            // NOTE: built against the current StreamPlayback shape (MediaModels.kt:68) —
            // mediaId/episodeId/streamUrl/format/qualities/audioTracks/subtitles.
            StreamPlayback(
                mediaId = subjectId,
                episodeId = if (episode > 0) "ep_${season}_${episode}" else null,
                streamUrl = stream.url,
                format = stream.type,  // "hls" or "mp4"
                qualities = listOf(PlaybackQuality(label = stream.quality, url = stream.url)),
                audioTracks = emptyList(),
                subtitles = captions.map {
                    SubtitleTrack(label = it.languageName, url = it.url, languageCode = it.language)
                }
            )
        } else {
            Log.e("StreamWave", "❌ Failed: All providers returned no valid streams")
            null
        }
    } catch (e: Exception) {
        Log.e("StreamWave", "❌ Error: ${e.message}", e)
        null
    }
}

/**
 * Composable hook to load streams with StreamWave
 * Returns UI state (loading, success, error) for player screens
 */
@Composable
fun rememberStreamWavePlayback(
    subjectId: String,
    season: Int = 0,
    episode: Int = 0
): StreamWavePlaybackState {
    val context = LocalContext.current
    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModelFactory(context)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val selectedStream by viewModel.selectedStream.collectAsState()
    val captions by viewModel.captions.collectAsState()
    
    LaunchedEffect(subjectId) {
        viewModel.loadStreams(subjectId, season, episode)
    }
    
    // Capture the delegated StateFlow value into a local val so it can smart-cast.
    val stream = selectedStream
    return when {
        stream != null && captions.isNotEmpty() -> {
            StreamWavePlaybackState.Ready(
                url = stream.url,
                quality = stream.quality,
                provider = stream.provider,
                type = stream.type,
                captionUrls = captions.map { it.url }
            )
        }
        stream != null -> {
            StreamWavePlaybackState.Ready(
                url = stream.url,
                quality = stream.quality,
                provider = stream.provider,
                type = stream.type,
                captionUrls = emptyList()
            )
        }
        uiState.toString().contains("Loading") -> StreamWavePlaybackState.Loading
        uiState.toString().contains("Error") -> StreamWavePlaybackState.Error("Failed to load streams")
        else -> StreamWavePlaybackState.Idle
    }
}

/**
 * State representation for StreamWave playback
 */
sealed class StreamWavePlaybackState {
    object Idle : StreamWavePlaybackState()
    object Loading : StreamWavePlaybackState()
    data class Ready(
        val url: String,
        val quality: String,
        val provider: String,
        val type: String,  // "hls" or "mp4"
        val captionUrls: List<String>
    ) : StreamWavePlaybackState()
    data class Error(val message: String) : StreamWavePlaybackState()
}

/**
 * Extension function to convert stream type to MIME type for ExoPlayer
 */
fun String.toMimeType(): String {
    return when (this.lowercase()) {
        "hls", "m3u8" -> "application/x-mpegURL"
        "mp4" -> "video/mp4"
        else -> "video/mp4"
    }
}

/**
 * Example: How to integrate StreamWave into existing player
 * 
 * // In your existing PlayerScreen:
 * LaunchedEffect(mediaId, episodeId) {
 *     // Use StreamWave to resolve streams first
 *     val streamPlayback = resolveStreamPlayback(context, mediaId, season, episode)
 *     if (streamPlayback != null) {
 *         // Pass to existing player
 *         initializeExoPlayer(streamPlayback.url, streamPlayback.type.toMimeType())
 *     } else {
 *         // Show error
 *     }
 * }
 * 
 * // Or use Compose hook:
 * val playbackState = rememberStreamWavePlayback(mediaId, season, episode)
 * when (playbackState) {
 *     is StreamWavePlaybackState.Ready -> {
 *         // Initialize player with playbackState.url
 *     }
 *     StreamWavePlaybackState.Loading -> {
 *         // Show loading
 *     }
 *     is StreamWavePlaybackState.Error -> {
 *         // Show error
 *     }
 * }
 */
