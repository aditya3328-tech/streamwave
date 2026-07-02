package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.FavoriteItem
import com.example.data.local.WatchlistItem
import com.example.data.model.MediaSummary
import com.example.ui.components.*
import com.example.ui.viewmodel.CineVerseViewModel
import com.example.ui.viewmodel.DetailsUiState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    mediaId: String,
    viewModel: CineVerseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String, String?) -> Unit,
    onNavigateToStaff: (String) -> Unit,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Load metadata every time the mediaId changes
    LaunchedEffect(mediaId) {
        viewModel.loadMediaDetails(mediaId)
    }

    val detailsState by viewModel.detailsUiState.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    AMOLEDCanvas(modifier = modifier) {
        when (detailsState) {
            is DetailsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CRUNCHY_ORANGE)
                }
            }
            is DetailsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text((detailsState as DetailsUiState.Error).message, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadMediaDetails(mediaId) },
                            colors = ButtonDefaults.buttonColors(containerColor = CRUNCHY_ORANGE)
                        ) {
                            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            is DetailsUiState.Success -> {
                val successData = detailsState as DetailsUiState.Success
                val details = successData.details
                val recommendations = successData.recommendations

                val imdbRatingValue = if (details.rating > 0.0) details.rating.toString() else ""
                val trailerUrl = details.trailerVideoAddressUrl ?: details.trailerUrl ?: ""

                // Before rendering UI log:
                android.util.Log.d("DETAIL_IMDB", imdbRatingValue)
                android.util.Log.d("DETAIL_TRAILER_URL", trailerUrl)

                val isWatchlistPresent = watchlist.any { it.mediaId == details.id }
                val isFavoritePresent = favorites.any { it.mediaId == details.id }
                val context = LocalContext.current

                // Fullscreen Trailer Player overlay state
                var isFullscreenPlayerOpen by remember { mutableStateOf(false) }

                // ExoPlayer instance configured for muted loop-forever background playback
                val lifecycleOwner = LocalLifecycleOwner.current
                val exoPlayer = remember(trailerUrl) {
                    ExoPlayer.Builder(context).build().apply {
                        repeatMode = Player.REPEAT_MODE_ALL
                        playWhenReady = true
                        volume = 0f // Muted by default
                        if (!trailerUrl.isNullOrEmpty()) {
                            setMediaItem(MediaItem.fromUri(trailerUrl))
                            prepare()
                        }
                    }
                }

                // Pause active background loop if fullscreen trailer player becomes active
                LaunchedEffect(isFullscreenPlayerOpen) {
                    if (isFullscreenPlayerOpen) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                }

                // Track and pause player when the application transitions to background, resume when active
                DisposableEffect(lifecycleOwner, exoPlayer) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_PAUSE -> {
                                exoPlayer.pause()
                            }
                            Lifecycle.Event.ON_RESUME -> {
                                if (!isFullscreenPlayerOpen) {
                                    exoPlayer.play()
                                }
                            }
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        exoPlayer.release()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. consolidated Netflix Hero Section
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(480.dp)
                            ) {
                                if (!trailerUrl.isNullOrEmpty()) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                useController = false
                                                player = exoPlayer
                                                setAspectRatioListener { _, _, _ -> }
                                            }
                                        },
                                        update = { view ->
                                            view.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    OptimizedPosterImage(
                                        data = details.posterPath.ifBlank { details.backdropPath },
                                        contentDescription = details.title,
                                        modifier = Modifier.fillMaxSize(),
                                        widthPx = 1280,
                                        heightPx = 720
                                    )
                                }

                                // Dark gradient overlay transitioning to solid black
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.5f),
                                                    Color.Transparent,
                                                    AMOLED_BLACK.copy(alpha = 0.8f),
                                                    AMOLED_BLACK
                                                ),
                                                startY = 0f,
                                                endY = Float.MAX_VALUE
                                            )
                                        )
                                )

                                // Navigation Back button
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier
                                        .statusBarsPadding()
                                        .padding(16.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }

                                // Info Overlay Column (Bottom of Hero)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 20.dp, vertical = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = details.title,
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 34.sp
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (details.rating > 0.0) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(CRUNCHY_ORANGE.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "IMDb Rating",
                                                    tint = CRUNCHY_ORANGE,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = details.rating.toString(),
                                                    color = CRUNCHY_ORANGE,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (details.year.isNotBlank()) {
                                            Text(
                                                text = details.year,
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (details.runtime.isNotBlank() && details.runtime != "0 min") {
                                            Text(text = "•", color = Color.Gray, fontSize = 12.sp)
                                            Text(
                                                text = details.runtime,
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (details.genres.isNotEmpty()) {
                                            Text(text = "•", color = Color.Gray, fontSize = 12.sp)
                                            Text(
                                                text = details.genres.first(),
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (details.genres.size > 1) {
                                        Text(
                                            text = details.genres.drop(1).joinToString(", "),
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                onNavigateToPlayer(details.id, null)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .testTag("detail_play_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = CRUNCHY_ORANGE,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Watch Now",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (!trailerUrl.isNullOrEmpty()) {
                                                    exoPlayer.volume = 1f
                                                    isFullscreenPlayerOpen = true
                                                }
                                            },
                                            enabled = !trailerUrl.isNullOrEmpty(),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .testTag("detail_trailer_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.White.copy(alpha = 0.15f),
                                                contentColor = Color.White,
                                                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                                                disabledContentColor = Color.Gray
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = if (!trailerUrl.isNullOrEmpty()) Color.White else Color.Gray
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Watch Trailer",
                                                color = if (!trailerUrl.isNullOrEmpty()) Color.White else Color.Gray,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Action buttons underneath the Hero Section
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.toggleWatchlist(
                                                MediaSummary(
                                                    details.id,
                                                    details.title,
                                                    details.posterPath,
                                                    details.backdropPath,
                                                    details.mediaType,
                                                    details.rating,
                                                    details.year,
                                                    details.genres
                                                )
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = if (isWatchlistPresent) CRUNCHY_ORANGE else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isWatchlistPresent) "In Watchlist" else "Add Watchlist",
                                            color = if (isWatchlistPresent) CRUNCHY_ORANGE else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.toggleFavorite(
                                                MediaSummary(
                                                    details.id,
                                                    details.title,
                                                    details.posterPath,
                                                    details.backdropPath,
                                                    details.mediaType,
                                                    details.rating,
                                                    details.year,
                                                    details.genres
                                                )
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isFavoritePresent) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (isFavoritePresent) CRUNCHY_ORANGE else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isFavoritePresent) "Favorited" else "Favorite",
                                            color = if (isFavoritePresent) CRUNCHY_ORANGE else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                    // 5. Synopsis Description Box
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Synopsis",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = details.description.ifBlank { "No synopsis is available for this show." },
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    // 6. Languages (Dubs) — MovieBox-style chips. Selecting a language reloads the
                    // whole detail screen in place (title/desc/seasons/episodes/cast/recs/streams)
                    // for the dub's subjectId. No navigation push/pop.
                    if (details.dubs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Languages",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(details.dubs, key = { it.subjectId }) { dub ->
                                    val isSelected = dub.subjectId == details.id
                                    Row(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) CRUNCHY_ORANGE else DEEP_GREY)
                                            .clickable {
                                                if (!isSelected) {
                                                    viewModel.loadMediaDetails(dub.subjectId)
                                                }
                                            }
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = dub.lanName ?: "Unknown",
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected language",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. Cast Section (Horizontal Circle List)
                    if (details.stars.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Cast Members",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(details.stars, key = { index, star -> "${star.staffId}_$index" }) { _, star ->
                                    Column(
                                        modifier = Modifier
                                            .width(76.dp)
                                            .clickable { onNavigateToStaff(star.staffId) },
                                        horizontalAlignment = Alignment.CenterHorizontally
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
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = star.name,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = star.character ?: "Actor",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 8. Seasons & Episode Resolutions Section
                    if (details.seasons.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Presentations & Seasons",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                details.seasons.forEach { season ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = if (season.se == 0) "Feature Presentation" else "Season ${season.se}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (season.maxEp != null && season.maxEp > 0) {
                                                Text(
                                                    text = "Episodes: ${season.maxEp}",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            if (!season.resolutions.isNullOrEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    season.resolutions.forEach { res ->
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(CRUNCHY_ORANGE.copy(alpha = 0.15f))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "${res.resolution}p (Ep ${res.epNum ?: 1})",
                                                                color = CRUNCHY_ORANGE,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 9. User Comments Reviews Section
                    if (details.comments.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Community Reviews (${details.comments.size})",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                        items(details.comments, key = { it.postId }) { comment ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.03f))
                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OptimizedPosterImage(
                                        data = comment.user?.avatar,
                                        contentDescription = comment.user?.nickname ?: "User",
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(DEEP_GREY),
                                        widthPx = 56,
                                        heightPx = 56
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = comment.user?.nickname ?: "Anonymous Fan",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = comment.createdAt?.let {
                                                try {
                                                    val instant = java.time.Instant.ofEpochMilli(it.toLong())
                                                    java.time.format.DateTimeFormatter.ISO_INSTANT.format(instant).substring(0, 10)
                                                } catch (e: Exception) {
                                                    "Recent Review"
                                                }
                                            } ?: "Recent Review",
                                            color = Color.Gray,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = comment.content ?: "",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // 10. Recommendations Section ("More Like This")
                    if (recommendations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalMediaRow(
                                title = "More Like This",
                                items = recommendations,
                                onItemClick = { onNavigateToDetails(it.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }

                // --- Fullscreen Trailer Player Integration ---
                if (isFullscreenPlayerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .testTag("fullscreen_trailer_player")
                    ) {
                        val fullscreenPlayer = remember(trailerUrl) {
                            ExoPlayer.Builder(context).build().apply {
                                repeatMode = Player.REPEAT_MODE_ALL
                                playWhenReady = true
                                volume = 1f // Unmuted
                                if (!trailerUrl.isNullOrEmpty()) {
                                    setMediaItem(MediaItem.fromUri(trailerUrl))
                                    prepare()
                                }
                            }
                        }

                        DisposableEffect(fullscreenPlayer) {
                            onDispose {
                                fullscreenPlayer.release()
                            }
                        }

                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = true
                                    player = fullscreenPlayer
                                }
                            },
                            update = { view ->
                                view.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Close button for Fullscreen modal
                        IconButton(
                            onClick = {
                                isFullscreenPlayerOpen = false
                                exoPlayer.volume = 0f
                            },
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(16.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
