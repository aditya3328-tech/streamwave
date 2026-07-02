@file:OptIn(UnstableApi::class)
package com.example.ui.components

import java.util.Locale
import android.content.res.Configuration
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.Caption
import com.example.data.model.Stream
import kotlinx.coroutines.delay

/**
 * StreamWave Video Player Component
 * Full-featured video player with HLS/MP4 support, quality selection, subtitles, and playback controls
 * 
 * Features:
 * - Responsive layout for Portrait and Landscape
 * - Center playback controls with Skip ±10s
 * - Quality, Speed, and Caption menus
 * - Dynamic scaling based on screen dimensions
 */
@Composable
fun StreamWaveVideoPlayer(
    streams: List<Stream> = emptyList(),
    captions: List<Caption> = emptyList(),
    selectedStream: Stream? = null,
    onStreamSelected: (Stream) -> Unit = {},
    onCaptionSelected: (Caption) -> Unit = {},
    onNextEpisode: () -> Unit = {},
    onPreviousEpisode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            selectedStream?.let { stream ->
                val mediaItem = MediaItem.fromUri(stream.url)
                setMediaItem(mediaItem)
                prepare()
            }
        }
    }
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showCaptionMenu by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf(selectedStream) }
    var selectedCaption by remember { mutableStateOf<Caption?>(null) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    
    val speedOptions = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlaying = playbackState == Player.STATE_READY && exoPlayer.isPlaying
            }
            
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }
    
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration
            delay(100)
        }
    }
    
    LaunchedEffect(showControls) {
        if (showControls && !showSpeedMenu && !showQualityMenu && !showCaptionMenu) {
            delay(3000)
            showControls = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.aspectRatio(16f / 9f))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = { offset ->
                        val tapPosition = offset.x / size.width
                        if (tapPosition < 0.3) {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                        } else if (tapPosition > 0.7) {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                        }
                    }
                )
            }
    ) {
        // ExoPlayer View
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Controls Overlay
        if (showControls) {
            VideoControls(
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                playbackSpeed = playbackSpeed,
                streams = streams,
                captions = captions,
                selectedQuality = selectedQuality,
                selectedCaption = selectedCaption,
                showSpeedMenu = showSpeedMenu,
                showQualityMenu = showQualityMenu,
                showCaptionMenu = showCaptionMenu,
                speedOptions = speedOptions,
                resizeMode = resizeMode,
                onPlayPause = {
                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                },
                onSeek = { position ->
                    exoPlayer.seekTo(position)
                },
                onRewind = {
                    exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                },
                onForward = {
                    exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                },
                onSpeedChange = { speed ->
                    playbackSpeed = speed
                    exoPlayer.setPlaybackSpeed(speed)
                    showSpeedMenu = false
                },
                onQualitySelect = { stream ->
                    selectedQuality = stream
                    onStreamSelected(stream)
                    val currentPos = exoPlayer.currentPosition
                    val mediaItem = MediaItem.fromUri(stream.url)
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(currentPos)
                    showQualityMenu = false
                },
                onCaptionSelect = { caption ->
                    selectedCaption = caption
                    onCaptionSelected(caption)
                    showCaptionMenu = false
                },
                onResizeToggle = {
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                onSpeedMenuToggle = { showSpeedMenu = it },
                onQualityMenuToggle = { showQualityMenu = it },
                onCaptionMenuToggle = { showCaptionMenu = it },
                onFullscreen = { /* Activity handling for orientation change */ },
                onNextEpisode = onNextEpisode,
                onPreviousEpisode = onPreviousEpisode
            )
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
}

@Composable
fun VideoControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float,
    streams: List<Stream>,
    captions: List<Caption>,
    selectedQuality: Stream?,
    selectedCaption: Caption?,
    showSpeedMenu: Boolean,
    showQualityMenu: Boolean,
    showCaptionMenu: Boolean,
    speedOptions: List<Float>,
    resizeMode: Int,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onQualitySelect: (Stream) -> Unit,
    onCaptionSelect: (Caption) -> Unit,
    onResizeToggle: () -> Unit,
    onSpeedMenuToggle: (Boolean) -> Unit,
    onQualityMenuToggle: (Boolean) -> Unit,
    onCaptionMenuToggle: (Boolean) -> Unit,
    onFullscreen: (Boolean) -> Unit,
    onNextEpisode: () -> Unit,
    onPreviousEpisode: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Dynamic sizing based on viewport width
    val playButtonSize = if (isLandscape) screenWidth * 0.12f else screenWidth * 0.08f
    val centerIconSize = if (isLandscape) screenWidth * 0.06f else screenWidth * 0.05f
    val actionIconSize = if (isLandscape) screenWidth * 0.045f else screenWidth * 0.038f
    val controlPadding = if (isLandscape) screenWidth * 0.02f else screenWidth * 0.015f
    val bottomMargin = if (isLandscape) screenHeight * 0.04f else screenHeight * 0.02f
    val fontSize = if (isLandscape) 14.sp else 12.sp

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient overlay
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
        )

        // 1. Center Controls: Rewind, Play/Pause, Forward
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 64.dp else 32.dp)
        ) {
            IconButton(onClick = onRewind, modifier = Modifier.size(centerIconSize)) {
                Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White, modifier = Modifier.fillMaxSize())
            }

            IconButton(onClick = onPlayPause, modifier = Modifier.size(playButtonSize)) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }

            IconButton(onClick = onForward, modifier = Modifier.size(centerIconSize)) {
                Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.fillMaxSize())
            }
        }

        // 2. Bottom Controls Layout
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = controlPadding)
                .padding(bottom = bottomMargin)
        ) {
            // Seek Bar
            ProgressBar(currentPosition = currentPosition, duration = duration, onSeek = onSeek)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Time Indicator
                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium
                )

                // Action Icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Episode Controls
                    IconButton(onClick = onPreviousEpisode, modifier = Modifier.size(actionIconSize)) {
                        Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White)
                    }
                    IconButton(onClick = onNextEpisode, modifier = Modifier.size(actionIconSize)) {
                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White)
                    }

                    // Captions Menu
                    if (captions.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { onCaptionMenuToggle(!showCaptionMenu) }, modifier = Modifier.size(actionIconSize)) {
                                Icon(Icons.Default.ClosedCaption, "CC", tint = Color.White)
                            }
                            DropdownMenu(expanded = showCaptionMenu, onDismissRequest = { onCaptionMenuToggle(false) }) {
                                captions.forEach { caption ->
                                    DropdownMenuItem(
                                        text = { Text(caption.languageName, fontWeight = if (caption == selectedCaption) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = { onCaptionSelect(caption) }
                                    )
                                }
                            }
                        }
                    }

                    // Quality Menu
                    Box {
                        IconButton(onClick = { onQualityMenuToggle(!showQualityMenu) }, modifier = Modifier.size(actionIconSize)) {
                            Icon(Icons.Default.Settings, "Quality", tint = Color.White)
                        }
                        DropdownMenu(expanded = showQualityMenu, onDismissRequest = { onQualityMenuToggle(false) }) {
                            streams.forEach { stream ->
                                DropdownMenuItem(
                                    text = { Text("${stream.quality} (${stream.type})", fontWeight = if (stream == selectedQuality) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { onQualitySelect(stream) }
                                )
                            }
                        }
                    }

                    // Speed Menu
                    Box {
                        IconButton(onClick = { onSpeedMenuToggle(!showSpeedMenu) }, modifier = Modifier.size(actionIconSize)) {
                            Icon(Icons.Default.Speed, "Speed", tint = Color.White)
                        }
                        DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { onSpeedMenuToggle(false) }) {
                            speedOptions.forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x", fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { onSpeedChange(speed) }
                                )
                            }
                        }
                    }

                    // Resize Toggle
                    IconButton(onClick = onResizeToggle, modifier = Modifier.size(actionIconSize)) {
                        val icon = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> Icons.Default.AspectRatio
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> Icons.Default.ZoomOutMap
                            else -> Icons.Default.FitScreen
                        }
                        Icon(icon, "Fit", tint = Color.White)
                    }

                    // Fullscreen Toggle
                    IconButton(onClick = { onFullscreen(!isLandscape) }, modifier = Modifier.size(actionIconSize)) {
                        Icon(if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, "Fullscreen", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressBar(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableLongStateOf(currentPosition) }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Slider(
        value = if (isDragging) dragPosition.toFloat() else currentPosition.toFloat(),
        onValueChange = {
            isDragging = true
            dragPosition = it.toLong()
        },
        onValueChangeFinished = {
            onSeek(dragPosition)
            isDragging = false
        },
        valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = Color.Red,
            activeTrackColor = Color.Red,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isLandscape) 40.dp else 32.dp)
    )
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
