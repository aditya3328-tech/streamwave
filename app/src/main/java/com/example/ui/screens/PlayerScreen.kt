package com.example.ui.screens
import android.util.Log
import androidx.media3.common.PlaybackException
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.view.WindowManager

import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import android.net.Uri
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.EpisodeSummary
import com.example.data.model.MediaDetails
import com.example.data.model.MediaSummary
import com.example.data.model.StreamPlayback
import com.example.ui.components.*
import com.example.ui.viewmodel.CineVerseViewModel
import com.example.ui.viewmodel.PlaybackUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    mediaId: String,
    episodeId: String?,
    viewModel: CineVerseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToPlayer: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Query stream configurations
    LaunchedEffect(mediaId, episodeId) {
        Log.d("SELECTED_EPISODE", "PlayerScreen triggered: mediaId=$mediaId episodeId=$episodeId")
        viewModel.loadPlayback(mediaId, episodeId)
    }

    // Keep screen active while viewing
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val playbackState by viewModel.playbackUiState.collectAsState()

    AMOLEDCanvas(modifier = modifier) {
        when (playbackState) {
            is PlaybackUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CRUNCHY_ORANGE)
                }
            }
            is PlaybackUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = (playbackState as PlaybackUiState.Error).message,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadPlayback(mediaId, episodeId) },
                            colors = ButtonDefaults.buttonColors(containerColor = CRUNCHY_ORANGE)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }
            is PlaybackUiState.Success -> {
                val successData = playbackState as PlaybackUiState.Success
                StreamingPlayer(
                    playback = successData.playback,
                    details = successData.mediaDetails,
                    recommendations = successData.recommendations,
                    initialEpisodeId = episodeId,
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack,
                    onNavigateToDetails = onNavigateToDetails,
                    onNavigateToPlayer = onNavigateToPlayer
                )
            }
            else -> {}
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun StreamingPlayer(
    playback: StreamPlayback,
    details: MediaDetails,
    recommendations: List<MediaSummary>,
    initialEpisodeId: String?,
    viewModel: CineVerseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToPlayer: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeEpisode = details.episodes.find { it.id == initialEpisodeId } ?: details.episodes.firstOrNull()

    // Log which episode is active
    Log.d("SELECTED_EPISODE", "Active episode: ${activeEpisode?.id} number=${activeEpisode?.number}")
    Log.d("PLAYER_URL", "Stream URL from playback: ${playback.streamUrl}")
    Log.d("STREAM_COUNT", "Quality options count: ${playback.qualities.size}")

    // Configure ExoPlayer Instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Playback state variables
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var scaleTypeFit by remember { mutableStateOf(true) }
    var playRate by remember { mutableStateOf(1.0f) }

    // Option menu variables
    var showTrackSelectionSheet by remember { mutableStateOf(false) }
    // BUG FIX: selectedQuality must start from the FIRST quality (highest), not firstOrNull which
    // could pick "Auto". We also keep it consistent with what ExoPlayer actually receives.
    var selectedQuality by remember(playback) {
        mutableStateOf(playback.qualities.firstOrNull()?.label ?: "Auto")
    }
    var selectedAudio by remember { mutableStateOf(playback.audioTracks.firstOrNull()?.label ?: "English") }

    val initialSub = remember(playback) {
        val engSub = playback.subtitles.find { it.label.contains("English", ignoreCase = true) }
        engSub?.label ?: playback.subtitles.firstOrNull()?.label ?: "None"
    }
    var selectedSubtitle by remember(playback) { mutableStateOf(initialSub) }

    // Trailer Dialog State
    var showTrailerDialog by remember { mutableStateOf(false) }

    // Season State
    var selectedSeasonSe by remember(details) {
        val currentSe = if (activeEpisode != null) {
            val parts = activeEpisode.id.split("_")
            if (parts.size >= 3 && parts[0] == "ep") parts[1].toIntOrNull() ?: 1 else 1
        } else 1
        mutableStateOf(currentSe)
    }

    // Gesture Values: Volume & Brightness
    var volumeLevel by remember { mutableStateOf(0.5f) }
    var brightnessLevel by remember { mutableStateOf(0.6f) }
    var currentGestureValue by remember { mutableStateOf<String?>(null) }

    // Auto-hide controls logic
    LaunchedEffect(showControls, lastInteractionTime) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    // ExoPlayer error listener for full debug visibility
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("EXOPLAYER_ERROR", "ExoPlayer error: ${error.errorCodeName} — ${error.message}", error)
                Log.e("EXOPLAYER_ERROR", "Cause: ${error.cause?.message}")
                Log.e("EXOPLAYER_ERROR", "Current URL: ${playback.streamUrl}")
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                val stateName = when (playbackState) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Log.d("EXOPLAYER_STATE", "State changed to: $stateName")
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Progress listener inside Player
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition
            durationMs = exoPlayer.duration.coerceAtLeast(0L)

            // Watch history persistence updates
            if (exoPlayer.isPlaying && durationMs > 0) {
                viewModel.updateWatchProgress(
                    mediaId = details.id,
                    title = details.title,
                    posterPath = details.posterPath,
                    mediaType = details.mediaType,
                    episodeId = activeEpisode?.id,
                    episodeNumber = activeEpisode?.number,
                    episodeTitle = activeEpisode?.title,
                    lastPositionMs = currentPositionMs,
                    durationMs = durationMs
                )
            }

            // Auto next episode trigger
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                val nextEpIndex = details.episodes.indexOf(activeEpisode) + 1
                if (nextEpIndex < details.episodes.size) {
                    val nextEp = details.episodes[nextEpIndex]
                    onNavigateToPlayer(details.id, nextEp.id)
                }
            }
            delay(1000)
        }
    }

    // BUG FIX: Reload ExoPlayer whenever the stream URL or quality changes.
    // Previously selectedQuality was in this LaunchedEffect key, but the URL
    // lookup used playback.qualities which is already resolved — this is correct.
    // The key fix: we compute streamUrlToUse from the resolved qualities map here
    // so that quality switching actually reloads with the new URL.
    LaunchedEffect(playback.mediaId, playback.episodeId, playback.streamUrl, selectedQuality) {
        val streamUrlToUse = playback.qualities.find { it.label == selectedQuality }?.url
            ?: playback.streamUrl

        Log.d("PLAYER_URL", "Loading into ExoPlayer: $streamUrlToUse")
        Log.d("SELECTED_QUALITY", "Quality selected: $selectedQuality")
        Log.d("SUBJECT_ID", "mediaId: ${playback.mediaId}")
        Log.d("DETAIL_PATH", "episodeId: ${playback.episodeId}")

        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        // BUG FIX: DefaultHttpDataSource headers must include Referer AND Origin,
        // exactly as the browser sends them. Missing these causes 403 on CDN.
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/149.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://movie-box.co/",
                    "Origin" to "https://movie-box.co",
                    "Accept" to "*/*"
                )
            )
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)

        // BUG FIX: Use HLS for .m3u8, otherwise ProgressiveMediaSource for MP4.
        // Also check playback.format field returned by the API.
        val mediaSource = if (
            playback.format.equals("hls", ignoreCase = true) ||
            streamUrlToUse.contains(".m3u8", ignoreCase = true)
        ) {
            Log.d("PLAYER_URL", "Using HlsMediaSource for: $streamUrlToUse")
            HlsMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrlToUse)))
        } else {
            Log.d("PLAYER_URL", "Using ProgressiveMediaSource (MP4) for: $streamUrlToUse")
            ProgressiveMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrlToUse)))
        }

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        // Retrieve persistent watch resume context
        val cachedRecord = viewModel.watchHistory.value.find { it.mediaId == details.id }
        if (cachedRecord != null && cachedRecord.lastPositionMs > 1000 && cachedRecord.lastPositionMs < (cachedRecord.durationMs - 5000)) {
            exoPlayer.seekTo(cachedRecord.lastPositionMs)
        }

        exoPlayer.playWhenReady = true
        isPlaying = true
    }

    // Toggle track subtitles inside ExoPlayer
    LaunchedEffect(selectedSubtitle, playback.subtitles) {
        val selectedSubObject = playback.subtitles.find { it.label == selectedSubtitle }
        if (selectedSubObject == null || selectedSubtitle == "None") {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(selectedSubObject.languageCode)
                .build()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Drag gestures for fast adjustment
    val gestureModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = { offset ->
                val screenWidth = size.width
                val leftHalf = offset.x < (screenWidth / 2)
                currentGestureValue = if (leftHalf) "Brightness" else "Volume"
                showControls = false
            },
            onDragEnd = {
                currentGestureValue = null
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                val delta = -dragAmount / 500f
                if (currentGestureValue == "Brightness") {
                    brightnessLevel = (brightnessLevel + delta).coerceIn(0.1f, 1.0f)
                    val activity = context as? Activity
                    val lp = activity?.window?.attributes
                    lp?.screenBrightness = brightnessLevel
                    activity?.window?.attributes = lp
                } else if (currentGestureValue == "Volume") {
                    volumeLevel = (volumeLevel + delta).coerceIn(0.0f, 1.0f)
                    exoPlayer.volume = volumeLevel
                }
            }
        )
    }

    // Main Column
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AMOLED_BLACK)
    ) {
        // Sticky 16:9 Video Player Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .then(gestureModifier)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        player = exoPlayer
                        controllerShowTimeoutMs = 3000
                    }
                },
                update = { view ->
                    view.resizeMode = if (scaleTypeFit) {
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        showControls = !showControls
                        if (showControls) lastInteractionTime = System.currentTimeMillis()
                    }
            )

            // Gesture overlays
            currentGestureValue?.let { gesture ->
                val valueToShow = if (gesture == "Brightness") brightnessLevel else volumeLevel
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (gesture == "Brightness") Icons.Default.BrightnessMedium else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = CRUNCHY_ORANGE,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${(valueToShow * 100).toInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // Custom UI Controls Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    // Top Bar: Back & Title
                    Row(
                        modifier = Modifier.align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = details.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Center: Controls (Double Tap Seek is implied by click handling, but we show buttons)
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Episode
                        val activeIndex = details.episodes.indexOf(activeEpisode)
                        val prevEp = if (activeIndex > 0) details.episodes[activeIndex - 1] else null
                        IconButton(
                            onClick = { if (prevEp != null) onNavigateToPlayer(details.id, prevEp.id) },
                            enabled = prevEp != null,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.SkipPrevious, "Prev", tint = if (prevEp != null) Color.White else Color.Gray, modifier = Modifier.size(32.dp))
                        }

                        // Play/Pause
                        IconButton(
                            onClick = {
                                isPlaying = !isPlaying
                                if (isPlaying) exoPlayer.play() else exoPlayer.pause()
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            modifier = Modifier.size(72.dp).background(CRUNCHY_ORANGE, CircleShape)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Next Episode
                        val nextEp = if (activeIndex >= 0 && activeIndex < details.episodes.size - 1) details.episodes[activeIndex + 1] else null
                        IconButton(
                            onClick = { if (nextEp != null) onNavigateToPlayer(details.id, nextEp.id) },
                            enabled = nextEp != null,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, "Next", tint = if (nextEp != null) Color.White else Color.Gray, modifier = Modifier.size(32.dp))
                        }
                    }

                    // Bottom: Timeline & Settings
                    Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatTime(currentPositionMs), color = Color.White, fontSize = 12.sp)
                            Slider(
                                value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                                onValueChange = { exoPlayer.seekTo((it * durationMs).toLong()) },
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                colors = SliderDefaults.colors(thumbColor = CRUNCHY_ORANGE, activeTrackColor = CRUNCHY_ORANGE)
                            )
                            Text(formatTime(durationMs), color = Color.White, fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { showTrackSelectionSheet = true }) {
                                Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Content Section
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Movie Info
            item {
                Column {
                    Text(details.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                        Text(" ${details.rating}  •  ${details.year}  •  ${details.genres.firstOrNull() ?: ""}  •  ${details.country}", color = Color.Gray, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        text = details.description,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (expanded) "Read Less" else "Read More",
                        color = CRUNCHY_ORANGE,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 4.dp)
                    )
                }
            }

            // TV Series Section (Seasons & Episodes)
            if (details.mediaType == "tv" || details.mediaType == "anime") {
                item {
                    Divider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 16.dp))
                    Column {
                        var showSeasonMenu by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DEEP_GREY)
                                    .clickable { showSeasonMenu = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Season $selectedSeasonSe", color = Color.White, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                            }
                            DropdownMenu(expanded = showSeasonMenu, onDismissRequest = { showSeasonMenu = false }, modifier = Modifier.background(DEEP_GREY)) {
                                details.seasons.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("Season ${s.se}", color = Color.White) },
                                        onClick = { selectedSeasonSe = s.se ?: 1; showSeasonMenu = false }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        val currentEpisodes = details.episodes.filter {
                            val parts = it.id.split("_")
                            (if (parts.size >= 3) parts[1].toIntOrNull() else 1) == selectedSeasonSe
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentEpisodes) { ep ->
                                val isActive = activeEpisode?.id == ep.id
                                Box(
                                    modifier = Modifier
                                        .size(60.dp, 40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) CRUNCHY_ORANGE else DEEP_GREY)
                                        .clickable {
                                            Log.d("SELECTED_EPISODE", "Episode tapped: ${ep.id} number=${ep.number}")
                                            onNavigateToPlayer(details.id, ep.id)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(ep.number.toString().padStart(2, '0'), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Cast Section
            if (details.cast.isNotEmpty()) {
                item {
                    Divider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 16.dp))
                    Text("Top Cast", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(details.cast) { staff ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                AsyncImage(
                                    model = staff.profilePath,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).clip(CircleShape).background(DEEP_GREY),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(staff.name, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // For You (Recommendations)
            if (recommendations.isNotEmpty()) {
                item {
                    Divider(color = Color.White.copy(0.1f), modifier = Modifier.padding(vertical = 16.dp))
                    Text("For You", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(recommendations) { rec ->
                            MovieCard(media = rec, onClick = { onNavigateToDetails(rec.id) }, modifier = Modifier.width(110.dp))
                        }
                    }
                }
            }
        }
    }

    // Settings Modal (Bottom Sheet style)
    if (showTrackSelectionSheet) {
        TrackSelectionDialog(
            playback = playback,
            selectedQuality = selectedQuality,
            onQualitySelect = { selectedQuality = it; showTrackSelectionSheet = false },
            selectedSubtitle = selectedSubtitle,
            onSubtitleSelect = { selectedSubtitle = it; showTrackSelectionSheet = false },
            onDismiss = { showTrackSelectionSheet = false }
        )
    }

    // Full screen overlay Trailer Dialog
    if (showTrailerDialog) {
        val trailerUrl = details.trailerVideoAddressUrl ?: details.trailerUrl
        if (!trailerUrl.isNullOrEmpty()) {
            TrailerPlayerDialog(
                url = trailerUrl,
                onDismiss = { showTrailerDialog = false }
            )
        }
    }
}

// BUG FIX: TrackSelectionDialog was missing its two closing braces, causing
// TrailerPlayerDialog to be parsed as a nested function inside it — compile error.
@Composable
fun TrackSelectionDialog(
    playback: StreamPlayback,
    selectedQuality: String,
    onQualitySelect: (String) -> Unit,
    selectedSubtitle: String,
    onSubtitleSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.7f)).clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DEEP_GREY, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp)
                .clickable(enabled = false) { }
        ) {
            Text("Video Quality", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                playback.qualities.forEach { q ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedQuality == q.label) CRUNCHY_ORANGE else Color.White.copy(0.1f))
                            .clickable { onQualitySelect(q.label) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(q.label, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Subtitles", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            val subOptions = listOf("None") + playback.subtitles.map { it.label }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subOptions) { sub ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedSubtitle == sub)
                                    CRUNCHY_ORANGE
                                else
                                    Color.White.copy(alpha = 0.1f)
                            )
                            .clickable { onSubtitleSelect(sub) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = sub,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun TrailerPlayerDialog(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val trailerPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            trailerPlayer.release()
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        player = trailerPlayer
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .align(Alignment.Center)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Trailer",
                    tint = Color.White
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
