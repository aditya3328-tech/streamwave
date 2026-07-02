package com.example.ui.screens

import android.app.Activity
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.EpisodeSummary
import com.example.data.model.MediaDetails
import com.example.data.model.MediaSummary
import com.example.data.model.StarModel
import com.example.data.model.StreamPlayback
import com.example.data.remote.MediaPlaybackHttpClient
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.AMOLEDCanvas
import com.example.ui.components.AMOLED_BLACK
import com.example.ui.components.CRUNCHY_ORANGE
import com.example.ui.components.DEEP_GREY
import com.example.ui.components.HorizontalMediaRow
import com.example.ui.components.OptimizedPosterImage
import com.example.ui.player.*
import com.example.ui.viewmodel.CineVerseViewModel
import com.example.ui.viewmodel.PlaybackUiState
import kotlinx.coroutines.delay

/**
 * Dedicated, modern OTT player screen. Resolves the stream via the (untouched) playback
 * pipeline, then renders a full-bleed ExoPlayer with reusable controls, fullscreen, PiP,
 * resize modes, gestures, and quality/subtitle selectors. It shows NO metadata — synopsis,
 * cast, and recommendations live exclusively on the Watch screen (DetailsScreen).
 */
@Composable
fun PlayerScreen(
    mediaId: String,
    episodeId: String?,
    viewModel: CineVerseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToPlayer: (String, String?) -> Unit,
    onNavigateToStaff: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LaunchedEffect(mediaId, episodeId) {
        Log.d("PLAYBACK_UI", "=== PLAYER INITIALIZED === mediaId=$mediaId episodeId=$episodeId")
        viewModel.loadPlayback(mediaId, episodeId)
    }

    // Keep the screen awake while the player is on screen.
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val playbackState by viewModel.playbackUiState.collectAsState()

    when (val state = playbackState) {
        is PlaybackUiState.Success -> {
            // Player fills the whole screen (its own black background).
            StreamingPlayer(
                playback = state.playback,
                details = state.mediaDetails,
                recommendations = state.recommendations,
                // Use the episode the playback state was actually resolved for (equal to the nav
                // arg in normal flows) so the UI stays consistent after a seamless dub switch,
                // which reloads playback for a new subject without changing the nav route.
                initialEpisodeId = state.playback.episodeId,
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onNavigateToPlayer = onNavigateToPlayer,
                onNavigateToDetails = onNavigateToDetails,
                onNavigateToStaff = onNavigateToStaff,
                modifier = modifier,
            )
        }
        is PlaybackUiState.Error -> {
            AMOLEDCanvas(modifier = modifier) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Text(state.message, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                        Button(
                            onClick = { viewModel.loadPlayback(mediaId, episodeId) },
                            colors = ButtonDefaults.buttonColors(containerColor = CRUNCHY_ORANGE),
                        ) { Text("Retry", color = Color.White) }
                    }
                }
            }
        }
        else -> {
            AMOLEDCanvas(modifier = modifier) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CRUNCHY_ORANGE)
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun StreamingPlayer(
    playback: StreamPlayback,
    details: MediaDetails,
    recommendations: List<MediaSummary>,
    initialEpisodeId: String?,
    viewModel: CineVerseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String, String?) -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToStaff: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val fullscreen = rememberFullscreenController()

    val activeEpisode = remember(details, initialEpisodeId) {
        details.episodes.find { it.id == initialEpisodeId } ?: details.episodes.firstOrNull()
    }
    val activeIndex = remember(details, activeEpisode) { details.episodes.indexOf(activeEpisode) }
    val hasPrev = activeIndex > 0
    val hasNext = activeIndex >= 0 && activeIndex < details.episodes.size - 1

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }

    // ---- UI / playback state ----
    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableStateOf(0L) }
    var isSpeedBoosted by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(PlayerResizeMode.FIT) }
    var pendingSeekMs by remember { mutableStateOf(0L) }

    // Premium UI states
    var isLocked by remember { mutableStateOf(false) }
    var currentSpeed by remember { mutableStateOf(1.0f) }
    var isDraggingSeek by remember { mutableStateOf(false) }

    var selectedQuality by remember(playback) {
        mutableStateOf(playback.qualities.firstOrNull()?.label ?: "Auto")
    }
    var selectedSubtitle by remember(playback) { mutableStateOf(SUBTITLE_OFF) }

    var showQualitySheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showResizeSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }

    // Gesture overlay (brightness / volume)
    var brightnessLevel by remember { mutableStateOf(0.6f) }
    var volumeLevel by remember { mutableStateOf(1.0f) }
    var gestureLabel by remember { mutableStateOf<String?>(null) }
    var gestureValue by remember { mutableStateOf(0f) }

    val pipSupported = remember { PiPManager.isPipSupported(context) }
    val configuration = LocalConfiguration.current
    val isInPip = remember(configuration) { activity?.isInPictureInPictureMode == true }

    val streamUrlToUse = remember(playback, selectedQuality) {
        playback.qualities.find { it.label == selectedQuality }?.url ?: playback.streamUrl
    }

    // Update speed on ExoPlayer
    LaunchedEffect(currentSpeed) {
        exoPlayer.setPlaybackSpeed(currentSpeed)
    }

    // ---- ExoPlayer diagnostics (preserved) ----
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("EXOPLAYER", "=== PLAYBACK ERROR === code=${error.errorCodeName} msg=${error.message}")
                var c: Throwable? = error.cause
                while (c != null && c !is HttpDataSource.InvalidResponseCodeException) c = c.cause
                (c as? HttpDataSource.InvalidResponseCodeException)?.let { httpEx ->
                    Log.e("EXOPLAYER", "HTTP ${httpEx.responseCode} for ${httpEx.dataSpec.uri}")
                    httpEx.headerFields.forEach { (k, v) -> Log.e("EXOPLAYER", "   $k: ${v.joinToString(", ")}") }
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                val name = when (state) {
                    Player.STATE_IDLE -> "STATE_IDLE"
                    Player.STATE_BUFFERING -> "STATE_BUFFERING"
                    Player.STATE_READY -> "STATE_READY"
                    Player.STATE_ENDED -> "STATE_ENDED"
                    else -> "STATE_UNKNOWN($state)"
                }
                Log.d("EXOPLAYER", "Playback state: $name")
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // ---- Build & load media. Re-runs on stream/quality/episode change (NOT on subtitle toggle). ----
    LaunchedEffect(playback.mediaId, playback.episodeId, streamUrlToUse, activeEpisode?.id) {
        // PRESERVED EXACTLY — Referer-locked CDN data source (movie-box.co Referer + OkHttp client).
        val fullDetailPath = details.detailPath?.takeIf { it.isNotBlank() }
        val cdnReferer = if (fullDetailPath != null) {
            "https://movie-box.co/movies/$fullDetailPath?id=${details.id}"
        } else {
            MediaPlaybackHttpClient.FALLBACK_REFERER
        }
        Log.d("EXOPLAYER", "=== MEDIA LOADING === url=$streamUrlToUse quality=$selectedQuality referer=$cdnReferer")

        val httpDataSourceFactory = OkHttpDataSource.Factory(MediaPlaybackHttpClient.client)
            .setUserAgent("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to cdnReferer,
                    "Origin" to "https://movie-box.co",
                    "Accept" to "*/*",
                ),
            )

        val videoItem = MediaItem.fromUri(Uri.parse(streamUrlToUse))
        val videoSource = if (
            playback.format.equals("hls", ignoreCase = true) ||
            streamUrlToUse.contains(".m3u8", ignoreCase = true)
        ) {
            HlsMediaSource.Factory(httpDataSourceFactory).createMediaSource(videoItem)
        } else {
            ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(videoItem)
        }

        // Sideload caption tracks (existing caption API) using the SAME Referer'd data source.
        val subtitleSources = playback.subtitles.mapNotNull { sub ->
            if (sub.url.isBlank()) return@mapNotNull null
            val cfg = MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.url))
                .setMimeType(detectSubtitleMime(sub.url))
                .setLanguage(sub.languageCode)
                .setLabel(sub.label)
                .build()
            SingleSampleMediaSource.Factory(httpDataSourceFactory).createMediaSource(cfg, C.TIME_UNSET)
        }

        val mediaSource = if (subtitleSources.isEmpty()) {
            videoSource
        } else {
            MergingMediaSource(*(listOf(videoSource) + subtitleSources).toTypedArray())
        }

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        // Resume order: quality switch (pendingSeekMs) → seamless dub switch (pendingResume) →
        // fresh load uses watch history.
        val pendingResume = viewModel.consumePendingResumePosition()
        if (pendingSeekMs > 0) {
            exoPlayer.seekTo(pendingSeekMs)
            pendingSeekMs = 0
        } else if (pendingResume > 0) {
            exoPlayer.seekTo(pendingResume)
        } else {
            val record = viewModel.watchHistory.value.find { it.mediaId == details.id }
            if (record != null &&
                record.lastEpisodeId == activeEpisode?.id &&
                record.lastPositionMs > 1000 &&
                record.lastPositionMs < (record.durationMs - 5000)
            ) {
                exoPlayer.seekTo(record.lastPositionMs)
            }
        }
        exoPlayer.playWhenReady = true
        isPlaying = true
    }

    // ---- Subtitle selection (no media rebuild) ----
    LaunchedEffect(selectedSubtitle, playback.subtitles) {
        val builder = exoPlayer.trackSelectionParameters.buildUpon()
        if (selectedSubtitle == SUBTITLE_OFF) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            playback.subtitles.find { it.label == selectedSubtitle }?.let {
                builder.setPreferredTextLanguage(it.languageCode)
            }
        }
        exoPlayer.trackSelectionParameters = builder.build()
    }

    // ---- Progress + watch-history persistence + auto-next (preserved) ----
    LaunchedEffect(exoPlayer) {
        while (true) {
            positionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            if (exoPlayer.isPlaying && durationMs > 0) {
                viewModel.updateWatchProgress(
                    mediaId = details.id,
                    title = details.title,
                    posterPath = details.posterPath,
                    mediaType = details.mediaType,
                    episodeId = activeEpisode?.id,
                    episodeNumber = activeEpisode?.number,
                    episodeTitle = activeEpisode?.title,
                    lastPositionMs = positionMs,
                    durationMs = durationMs,
                )
            }
            if (exoPlayer.playbackState == Player.STATE_ENDED && hasNext) {
                details.episodes.getOrNull(activeIndex + 1)?.let { onNavigateToPlayer(details.id, it.id) }
            }
            delay(1000)
        }
    }

    // Auto-hide controls state machine (7-second timeout)
    val isSheetOpen = showQualitySheet || showSubtitleSheet || showResizeSheet || showSpeedSheet || showLanguageSheet
    LaunchedEffect(showControls, lastInteraction, isPlaying, isSheetOpen, isSpeedBoosted, isLocked, isDraggingSeek, isInPip) {
        if (showControls && !isInPip) {
            // Never auto-hide when paused, sheet open, speed boosted, or dragging seek
            if (isPlaying && !isSheetOpen && !isSpeedBoosted && !isLocked && !isDraggingSeek) {
                delay(AUTO_HIDE_MS)
                showControls = false
            } else if (isLocked) {
                // In locked mode, hide lock button after timeout too
                delay(AUTO_HIDE_MS)
                showControls = false
            }
        }
    }

    // Let MainActivity.onUserLeaveHint() auto-enter PiP on Home press while actively playing.
    LaunchedEffect(isPlaying, pipSupported) {
        PiPManager.autoEnterEnabled = pipSupported && isPlaying
    }

    DisposableEffect(Unit) {
        onDispose {
            PiPManager.autoEnterEnabled = false
            exoPlayer.release()
        }
    }

    // Back exits fullscreen first, then navigates away.
    BackHandler(enabled = fullscreen.isFullscreen) { fullscreen.exit() }

    val windowSizeClass = rememberWindowSizeClass()
    val isTablet = windowSizeClass == PlayerWindowSizeClass.MEDIUM || windowSizeClass == PlayerWindowSizeClass.EXPANDED
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val immersive = (isLandscape && !isTablet) || fullscreen.isFullscreen || isInPip
    val showDetails = !immersive

    // Hide the system bars whenever the player owns the full screen (incl. auto-landscape).
    val localView = LocalView.current
    LaunchedEffect(immersive) {
        val act = activity ?: return@LaunchedEffect
        val barsController = WindowInsetsControllerCompat(act.window, localView)
        if (immersive) {
            barsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            barsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            barsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // The player surface (video + gestures + overlays + controls)
    val playerSurface: @Composable (Modifier) -> Unit = { surfaceModifier ->
        Box(
            modifier = surfaceModifier
                .background(PLAYER_BG)
                .pointerInput(Unit) {
                    // Pinch to zoom: toggle resize mode between FIT and ZOOM
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom > 1.05f) {
                            resizeMode = PlayerResizeMode.ZOOM
                        } else if (zoom < 0.95f) {
                            resizeMode = PlayerResizeMode.FIT
                        }
                    }
                },
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        player = exoPlayer
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { view ->
                    view.resizeMode = when (resizeMode) {
                        PlayerResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        PlayerResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        PlayerResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (!isInPip) {
                // Transparent gesture catcher (under the controls; empty taps fall through to it).
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .playerGestures(
                            onSingleTap = {
                                showControls = !showControls
                                if (showControls) lastInteraction = System.currentTimeMillis()
                            },
                            onSeekBack = {
                                if (!isLocked) {
                                    exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                                    lastInteraction = System.currentTimeMillis()
                                }
                            },
                            onSeekForward = {
                                if (!isLocked) {
                                    val max = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                                    exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(max))
                                    lastInteraction = System.currentTimeMillis()
                                }
                            },
                            onSpeedBoostStart = {
                                if (!isLocked) {
                                    exoPlayer.setPlaybackSpeed(2f)
                                    isSpeedBoosted = true
                                }
                            },
                            onSpeedBoostEnd = {
                                if (!isLocked) {
                                    exoPlayer.setPlaybackSpeed(currentSpeed)
                                    isSpeedBoosted = false
                                }
                            },
                            onDragStart = { side ->
                                if (!isLocked) {
                                    gestureLabel = if (side == GestureSide.LEFT) "Brightness" else "Volume"
                                    showControls = false
                                }
                            },
                            onVerticalDrag = { side, delta ->
                                if (!isLocked) {
                                    if (side == GestureSide.LEFT) {
                                        brightnessLevel = (brightnessLevel + delta).coerceIn(0.05f, 1f)
                                        activity?.window?.let { w ->
                                            w.attributes = w.attributes.apply { screenBrightness = brightnessLevel }
                                        }
                                        gestureValue = brightnessLevel
                                    } else {
                                        volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                                        exoPlayer.volume = volumeLevel
                                        gestureValue = volumeLevel
                                    }
                                }
                            },
                            onDragEnd = { gestureLabel = null },
                        ),
                )

                // Brightness / volume overlay (glassmorphic styling, 24dp corners)
                gestureLabel?.let { label ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .glassSurface(cornerRadius = GLASS_CORNER)
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (label == "Brightness") Icons.Default.BrightnessMedium else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = CRUNCHY_ORANGE,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("${(gestureValue * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(animationSpec = tween(FADE_ANIM_MS)),
                    exit = fadeOut(animationSpec = tween(FADE_ANIM_MS))
                ) {
                    PlayerControls(
                        title = details.title,
                        isPlaying = isPlaying,
                        isFullscreen = fullscreen.isFullscreen,
                        isSpeedBoosted = isSpeedBoosted,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        selectedQualityLabel = selectedQuality,
                        currentSpeedLabel = if (currentSpeed == 1.0f) "Normal" else "${currentSpeed}x",
                        pipSupported = pipSupported,
                        isLocked = isLocked,
                        hasPrev = hasPrev,
                        hasNext = hasNext,
                        onBack = { if (fullscreen.isFullscreen) fullscreen.exit() else onNavigateBack() },
                        onPlayPause = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            lastInteraction = System.currentTimeMillis()
                        },
                        onSeekBack = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0)); lastInteraction = System.currentTimeMillis() },
                        onSeekForward = {
                            val max = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(max))
                            lastInteraction = System.currentTimeMillis()
                        },
                        onSeekTo = {
                            isDraggingSeek = true
                            exoPlayer.seekTo(it)
                        },
                        onSeekFinished = {
                            isDraggingSeek = false
                            lastInteraction = System.currentTimeMillis()
                        },
                        onPrevEpisode = { if (hasPrev) details.episodes.getOrNull(activeIndex - 1)?.let { onNavigateToPlayer(details.id, it.id) } },
                        onNextEpisode = { if (hasNext) details.episodes.getOrNull(activeIndex + 1)?.let { onNavigateToPlayer(details.id, it.id) } },
                        onToggleSubtitles = { showSubtitleSheet = true },
                        onOpenQuality = { showQualitySheet = true },
                        onOpenResize = { showResizeSheet = true },
                        showLanguage = details.dubs.isNotEmpty(),
                        onOpenLanguage = { showLanguageSheet = true },
                        onToggleFullscreen = { fullscreen.toggle(); lastInteraction = System.currentTimeMillis() },
                        onEnterPip = { PiPManager.enterPip(activity) },
                        onOpenSpeed = { showSpeedSheet = true },
                        onLockToggle = {
                            isLocked = !isLocked
                            if (isLocked) {
                                showControls = true // Reset timeout for showing just the unlock button
                            }
                            lastInteraction = System.currentTimeMillis()
                        }
                    )
                }
            }
        }
    }

    if (showDetails) {
        if (isTablet) {
            // ---- Tablet Mode Layout: Side-by-side ----
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .background(PLAYER_BG)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    playerSurface(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { WatchInfoSection(details = details, activeEpisode = activeEpisode) }
                    
                    if (details.episodes.isNotEmpty() && !details.mediaType.equals("movie", ignoreCase = true)) {
                        item {
                            WatchEpisodeSelector(
                                details = details,
                                activeEpisode = activeEpisode,
                                onEpisodeSelected = { episode ->
                                    if (episode.id != activeEpisode?.id) {
                                        onNavigateToPlayer(details.id, episode.id)
                                    }
                                }
                            )
                        }
                    }
                    
                    if (details.stars.isNotEmpty()) {
                        item { WatchCastRow(stars = details.stars, onStaffClick = onNavigateToStaff) }
                    }
                    
                    if (recommendations.isNotEmpty()) {
                        item {
                            HorizontalMediaRow(
                                title = "Recommendations",
                                items = recommendations,
                                onItemClick = { onNavigateToDetails(it.id) }
                            )
                        }
                    }
                }
            }
        } else {
            // ---- YouTube-style portrait layout: pinned 16:9 player + scrolling details ----
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(PLAYER_BG)
                    .statusBarsPadding(),
            ) {
                playerSurface(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f), // Strict 16:9 aspect ratio
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    item { WatchInfoSection(details = details, activeEpisode = activeEpisode) }
                    if (details.episodes.isNotEmpty() && !details.mediaType.equals("movie", ignoreCase = true)) {
                        item {
                            WatchEpisodeSelector(
                                details = details,
                                activeEpisode = activeEpisode,
                                onEpisodeSelected = { episode ->
                                    if (episode.id != activeEpisode?.id) {
                                        onNavigateToPlayer(details.id, episode.id)
                                    }
                                },
                            )
                        }
                    }
                    if (details.stars.isNotEmpty()) {
                        item { WatchCastRow(stars = details.stars, onStaffClick = onNavigateToStaff) }
                    }
                    if (recommendations.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(20.dp))
                            HorizontalMediaRow(
                                title = "Recommendations",
                                items = recommendations,
                                onItemClick = { onNavigateToDetails(it.id) },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    } else {
        // ---- Fullscreen layout: the player owns the entire screen ----
        playerSurface(modifier.fillMaxSize())
    }

    // ---- Bottom sheets ----
    if (showQualitySheet) {
        QualitySelectorSheet(
            qualities = playback.qualities,
            selectedLabel = selectedQuality,
            onSelect = { quality ->
                if (quality.label != selectedQuality) {
                    pendingSeekMs = exoPlayer.currentPosition
                    selectedQuality = quality.label
                }
                showQualitySheet = false
            },
            onDismiss = { showQualitySheet = false },
        )
    }
    if (showSubtitleSheet) {
        SubtitleSelectorSheet(
            subtitles = playback.subtitles,
            selectedLabel = selectedSubtitle,
            onSelect = { label -> selectedSubtitle = label; showSubtitleSheet = false },
            onDismiss = { showSubtitleSheet = false },
        )
    }
    if (showResizeSheet) {
        ResizeModeSheet(
            current = resizeMode,
            onSelect = { mode -> resizeMode = mode; showResizeSheet = false },
            onDismiss = { showResizeSheet = false },
        )
    }
    if (showSpeedSheet) {
        SpeedSelectorSheet(
            selectedSpeed = currentSpeed,
            onSelect = { speed -> currentSpeed = speed; showSpeedSheet = false },
            onDismiss = { showSpeedSheet = false }
        )
    }
    if (showLanguageSheet) {
        AudioLanguageSheet(
            dubs = details.dubs,
            currentSubjectId = details.id,
            onSelect = { dub ->
                showLanguageSheet = false
                if (dub.subjectId != details.id) {
                    // Seamless switch: remember position, reload the new dub's stream for the
                    // same episode (S1E1 fallback handled in the ViewModel), then restore.
                    viewModel.switchAudioLanguage(
                        newSubjectId = dub.subjectId,
                        desiredEpisodeId = activeEpisode?.id,
                        positionMs = exoPlayer.currentPosition,
                    )
                }
            },
            onDismiss = { showLanguageSheet = false },
        )
    }
}

/**
 * Title + metadata (rating • year • runtime • genre) + collapsible synopsis.
 * Rendered directly under the player in portrait — no separate detail page.
 */
@Composable
private fun WatchInfoSection(details: MediaDetails, activeEpisode: EpisodeSummary?) {
    var descriptionExpanded by remember(details.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = details.title,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp,
        )

        // Restored Season / Episode info — TV shows / web series only, directly below the
        // title and above the description. Movies (no episode) are unaffected. The season is
        // derived from the existing episode id pattern "ep_<season>_<episode>".
        if (activeEpisode != null && !details.mediaType.equals("movie", ignoreCase = true)) {
            val seasonNumber = activeEpisode.id.split("_").getOrNull(1)?.toIntOrNull()
            val seasonEpisodeLabel = if (seasonNumber != null) {
                "Season $seasonNumber • Episode ${activeEpisode.number}"
            } else {
                "Episode ${activeEpisode.number}"
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = seasonEpisodeLabel,
                color = CRUNCHY_ORANGE,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )

            // Show the episode title only when it's a real one (not the generic "Episode N").
            if (activeEpisode.title.isNotBlank() &&
                activeEpisode.title != "Episode ${activeEpisode.number}"
            ) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = activeEpisode.title,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Rating • Year • Runtime • Genre
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (details.rating > 0.0) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = CRUNCHY_ORANGE,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = details.rating.toString(),
                    color = CRUNCHY_ORANGE,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                WatchMetaDot()
            }
            if (details.year.isNotBlank()) {
                Text(details.year, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (details.runtime.isNotBlank() && details.runtime != "0 min") {
                WatchMetaDot()
                Text(details.runtime, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            if (details.genres.isNotEmpty()) {
                WatchMetaDot()
                Text(
                    text = details.genres.first(),
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val synopsis = details.description.ifBlank { "No synopsis is available." }
        Text(
            text = synopsis,
            color = Color.LightGray,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (details.description.isNotBlank()) {
            Text(
                text = if (descriptionExpanded) "Show less" else "Read more",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clickable { descriptionExpanded = !descriptionExpanded },
            )
        }
    }
}

/**
 * MovieBox-style season switcher + horizontal episode chips.
 *
 * Pure UI over data that already exists on [MediaDetails]: seasons/episode counts come from
 * `details.episodes` (built upstream with ids "ep_<season>_<episode>"). Selecting an episode is
 * delegated to [onEpisodeSelected], which navigates to the same watch route — no new APIs, no
 * repository changes, and no playback wiring is involved.
 */
@Composable
private fun WatchEpisodeSelector(
    details: MediaDetails,
    activeEpisode: EpisodeSummary?,
    onEpisodeSelected: (EpisodeSummary) -> Unit,
) {
    // Group the flat episode list by the season encoded in each id ("ep_<season>_<episode>").
    val episodesBySeason = remember(details.episodes) {
        details.episodes.groupBy { it.id.split("_").getOrNull(1)?.toIntOrNull() ?: 1 }
    }
    val seasonNumbers = remember(episodesBySeason) { episodesBySeason.keys.sorted() }
    if (seasonNumbers.isEmpty()) return

    val activeSeason = activeEpisode?.id?.split("_")?.getOrNull(1)?.toIntOrNull()

    // Survives rotation / config changes. Keyed by media id so a different title starts fresh.
    var selectedSeason by rememberSaveable(details.id) {
        mutableStateOf(activeSeason ?: seasonNumbers.first())
    }

    // Keep the visible season in sync when playback crosses a season boundary (e.g. auto-next
    // from S1E10 to S2E1), without overriding a season the user is manually browsing.
    LaunchedEffect(activeSeason) {
        if (activeSeason != null && activeSeason != selectedSeason && episodesBySeason.containsKey(activeSeason)) {
            selectedSeason = activeSeason
        }
    }

    val effectiveSeason = if (episodesBySeason.containsKey(selectedSeason)) selectedSeason else seasonNumbers.first()
    val episodesForSeason = episodesBySeason[effectiveSeason].orEmpty()
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // ---- Season selector ([ Season 01 ▼ ]). Always shown; opens a dropdown when >1 season.
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DEEP_GREY)
                    .clickable(enabled = seasonNumbers.size > 1) { menuExpanded = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Season %02d".format(effectiveSeason),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (seasonNumbers.size > 1) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select season",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(DEEP_GREY),
            ) {
                seasonNumbers.forEach { season ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Season %02d".format(season),
                                color = if (season == effectiveSeason) CRUNCHY_ORANGE else Color.White,
                                fontWeight = if (season == effectiveSeason) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            selectedSeason = season
                            menuExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Episode chips: [01] [02] [03] … current = orange, others = dark grey.
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(episodesForSeason, key = { it.id }) { episode ->
                val isSelected = episode.id == activeEpisode?.id
                val chipColor by animateColorAsState(
                    targetValue = if (isSelected) CRUNCHY_ORANGE else DEEP_GREY,
                    label = "episode_chip_bg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.LightGray,
                    label = "episode_chip_fg",
                )
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(chipColor)
                        .clickable { onEpisodeSelected(episode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "%02d".format(episode.number),
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchMetaDot() {
    Text(
        text = "  •  ",
        color = Color.Gray,
        fontSize = 12.sp,
    )
}

/** Horizontal cast strip (circular avatars) — tapping a face opens that staff member. */
@Composable
private fun WatchCastRow(stars: List<StarModel>, onStaffClick: (String) -> Unit) {
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Cast",
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(stars, key = { index, star -> "${star.staffId}_$index" }) { _, star ->
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clickable { onStaffClick(star.staffId) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OptimizedPosterImage(
                    data = star.avatarUrl,
                    contentDescription = star.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(DEEP_GREY),
                    widthPx = 150,
                    heightPx = 150
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = star.name,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = star.character ?: "Actor",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun detectSubtitleMime(url: String): String = when {
    url.contains(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
    url.contains(".ass", ignoreCase = true) || url.contains(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
    url.contains(".ttml", ignoreCase = true) || url.contains(".dfxp", ignoreCase = true) -> MimeTypes.APPLICATION_TTML
    else -> MimeTypes.TEXT_VTT
}
