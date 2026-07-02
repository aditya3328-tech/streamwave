package com.example.sports.player

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.sports.api.SportsInterceptor

/** Coarse playback state surfaced to the overlay UI. */
data class SportsPlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val positionMs: Long = 0L,
    val error: String? = null,
)

/**
 * Standalone live ExoPlayer for the Sports module. Built fresh here — it does NOT reuse the
 * movie player, its data source, or its controls.
 *
 * HLS playback runs through [DefaultHttpDataSource.Factory] with the SportsNow-required
 * Origin / Referer / User-Agent headers, exactly as specified.
 *
 * @param streamUrl current channel/stream url ([com.example.sports.model.Match.playPath] or a
 *   selected [com.example.sports.model.PlaySource.url]). Changing it re-prepares the player.
 */
@OptIn(UnstableApi::class)
@Composable
fun SportsPlayer(
    streamUrl: String,
    modifier: Modifier = Modifier,
    onStateChange: (SportsPlaybackState) -> Unit = {},
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    var state by remember { mutableStateOf(SportsPlaybackState()) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state = state.copy(isPlaying = isPlaying)
                onStateChange(state)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                state = state.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
                onStateChange(state)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("SPORTS_PLAYER", "playback error: ${error.errorCodeName} ${error.message}")
                state = state.copy(error = error.message)
                onStateChange(state)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // (Re)build the media source whenever the selected stream changes.
    LaunchedEffect(streamUrl) {
        if (streamUrl.isBlank()) return@LaunchedEffect

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(SportsInterceptor.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "Origin" to SportsInterceptor.ORIGIN,
                    "Referer" to SportsInterceptor.REFERER,
                    "User-Agent" to SportsInterceptor.USER_AGENT,
                ),
            )

        val item = MediaItem.fromUri(Uri.parse(streamUrl))
        val source = if (streamUrl.contains(".m3u8", ignoreCase = true)) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(item)
        }

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaSource(source)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
