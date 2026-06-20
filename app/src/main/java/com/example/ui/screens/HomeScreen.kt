package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.WatchHistoryItem
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.viewmodel.CineVerseViewModel

@Composable
fun HomeScreen(
    viewModel: CineVerseViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToPlayer: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHomeLoading by viewModel.isHomeLoading.collectAsState()
    val homeSections by viewModel.homeSections.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val banners by viewModel.bannerItems.collectAsState()

    // Log the current list of banners for debug tracking
    LaunchedEffect(banners) {
        android.util.Log.d("BANNER_COUNT", banners.size.toString())
        banners.forEach { banner ->
            android.util.Log.d("BANNER_TITLE", banner.title)
        }
    }

    AMOLEDCanvas(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp) // safe target for bottom nav bar
        ) {
            // Skeletons when loading and sections are empty (First launch scenario)
            if (isHomeLoading && homeSections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CRUNCHY_ORANGE)
                    }
                }
                items(3) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SkeletonShimmer(width = 180.dp, height = 24.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(3) {
                                SkeletonShimmer(width = 120.dp, height = 160.dp)
                            }
                        }
                    }
                }
            }

            // Connection Empty State with Retry option
            if (!isHomeLoading && homeSections.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Connection Timed Out",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Could not retrieve live video feed from server. Please verify your connection status and snap retry.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadHomeData() },
                            colors = ButtonDefaults.buttonColors(containerColor = CRUNCHY_ORANGE)
                        ) {
                            Text("Retry Connection", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Continue Watching Row (Persistent Room progress tracks)
            if (watchHistory.isNotEmpty()) {
                item {
                    ContinueWatchingRow(
                        historyItems = watchHistory,
                        onItemClick = { item ->
                            onNavigateToPlayer(item.mediaId, item.lastEpisodeId)
                        }
                    )
                }
            }

            // CMS Dynamic Renderer
            items(homeSections) { section ->
                when (section.type) {
                    "BANNER" -> {
                        val context = LocalContext.current
                        DynamicHeroCarousel(
                            banners = banners,
                            onBannerClick = { banner ->
                                val subjectId = banner.subjectId ?: "0"
                                if (subjectId != "0" && subjectId.isNotBlank()) {
                                    onNavigateToDetails(subjectId)
                                } else {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(banner.url ?: "")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.util.Log.e("BANNER_CLICK", "Error launching external url: " + banner.url, e)
                                    }
                                }
                            }
                        )
                    }
                    "FILTER" -> {
                        FilterSection(
                            section = section,
                            onFilterClick = { query ->
                                val cleanedQuery = query.lowercase()
                                when {
                                    cleanedQuery.contains("japan") -> {
                                        viewModel.setMoviesFilter("Japan")
                                        viewModel.selectTab(2)
                                    }
                                    cleanedQuery.contains("india") -> {
                                        viewModel.setMoviesFilter("India")
                                        viewModel.selectTab(2)
                                    }
                                    cleanedQuery.contains("united states") -> {
                                        viewModel.setMoviesFilter("United States")
                                        viewModel.selectTab(2)
                                    }
                                    cleanedQuery.contains("korea") -> {
                                        viewModel.setTvFilter("Korea")
                                        viewModel.selectTab(3)
                                    }
                                    cleanedQuery.contains("china") -> {
                                        viewModel.setTvFilter("China")
                                        viewModel.selectTab(3)
                                    }
                                    cleanedQuery.contains("turkey") -> {
                                        viewModel.setTvFilter("Turkey")
                                        viewModel.selectTab(3)
                                    }
                                    else -> {
                                        viewModel.selectTab(1) // search screen
                                    }
                                }
                            }
                        )
                    }
                    "SUBJECTS_MOVIE", "SUBJECTS_TV", "SUBJECTS_ANIME", "SUBJECTS_DRAMA", "CUSTOM", "APPOINTMENT_LIST" -> {
                        SubjectsMovieSection(
                            section = section,
                            onItemClick = { id -> onNavigateToDetails(id) }
                        )
                    }
                    "SPORT_LIVE" -> {
                        SportsLiveSection(
                            section = section,
                            onMatchClick = { match ->
                                android.util.Log.d("SPORTS", "Match selected: " + match.matchId)
                            }
                        )
                    }
                    else -> {
                        // Silent fail / logged debugging step for unrecognized widgets
                        LaunchedEffect(section) {
                            android.util.Log.d("HOME_UNKNOWN_SECTION", "Skipped rendering for section type: ${section.type}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicHeroCarousel(
    banners: List<BannerItem>,
    onBannerClick: (BannerItem) -> Unit
) {
    if (banners.isEmpty()) return

    LaunchedEffect(banners) {
        android.util.Log.d("BANNER_COUNT", banners.size.toString())
        banners.forEach { banner ->
            android.util.Log.d("BANNER_TITLE", banner.title)
        }
    }

    var activeIndex by remember(banners) { mutableStateOf(0) }

    // Auto-scroll loop every 5 seconds if multiple banners exist
    LaunchedEffect(banners) {
        while (banners.size > 1) {
            kotlinx.coroutines.delay(5000L)
            activeIndex = (activeIndex + 1) % banners.size
        }
    }

    val activeItem = banners[activeIndex]

    // Print active banner title render trace
    DisposableEffect(activeItem) {
        android.util.Log.d("BANNER_TITLE_RENDER", activeItem.title)
        onDispose {}
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(480.dp)
            .clickable { onBannerClick(activeItem) }
    ) {
        // Backdrop Image with animation
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(activeItem.image?.url)
                    .crossfade(600)
                    .build(),
                contentDescription = activeItem.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Custom Gradient Overlays for premium AMOLED blending
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                AMOLED_BLACK
                            ),
                            startY = 0f,
                            endY = Float.MAX_VALUE
                        )
                    )
            )
        }

        // Title and controls at the bottom of the banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Indicator dots
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                banners.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (idx == activeIndex) 10.dp else 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (idx == activeIndex) CRUNCHY_ORANGE else Color.Gray.copy(alpha = 0.6f))
                    )
                }
            }

            // Genre chips
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val genres = activeItem.subject?.genre?.split(",") ?: emptyList()
                genres.take(3).forEach { genre ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(genre.trim(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                activeItem.subject?.corner?.let { corner ->
                    if (corner.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CRUNCHY_ORANGE.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(corner, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Title
            val cornerText = activeItem.subject?.corner
            val displayedTitle = if (!cornerText.isNullOrBlank()) {
                "${activeItem.title} [$cornerText]"
            } else {
                activeItem.title
            }

            Text(
                text = displayedTitle,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            activeItem.subject?.releaseDate?.take(4)?.let { year ->
                Text(
                    text = "Release Year: $year  |  IMDb: ★ ${activeItem.subject.imdbRatingValue ?: "N/A"}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }

            // Play & Info Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                Button(
                    onClick = { onBannerClick(activeItem) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NETFLIX_RED),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play Now", color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Details Button (Glassmorphic)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onBannerClick(activeItem) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Info", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    section: HomeSection,
    onFilterClick: (String) -> Unit
) {
    if (section.filters.isEmpty()) return

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)) {
        Text(
            text = section.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(section.filters) { filter ->
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DEEP_GREY)
                        .clickable { onFilterClick(filter.query ?: filter.title) }
                ) {
                    // Image banner behind filter text
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(filter.image?.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Dim/Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                    // Filter Title centered
                    Text(
                        text = filter.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectsMovieSection(
    section: HomeSection,
    onItemClick: (String) -> Unit
) {
    val subjects = if (section.subjects.isNotEmpty()) {
        section.subjects
    } else {
        section.customData?.items?.mapNotNull { it.subject } ?: emptyList()
    }

    if (subjects.isEmpty()) return

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)) {
        Text(
            text = section.title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(subjects) { subject ->
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DEEP_GREY)
                        .clickable { onItemClick(subject.subjectId) }
                ) {
                    Column {
                        // Cover poster image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(subject.cover?.url)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // IMDb rating value overlay (stars)
                            val rating = subject.imdbRatingValue
                            if (!rating.isNullOrBlank() && rating != "0" && rating != "0.0") {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "★ $rating",
                                        color = Color.Yellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Corner badge overlay (e.g. Hindi, CAM, Telugu)
                            val corner = subject.corner
                            if (!corner.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .clip(RoundedCornerShape(topEnd = 8.dp))
                                        .background(CRUNCHY_ORANGE)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = corner,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Title & Metadata
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = subject.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val releaseText = subject.releaseDate?.take(4) ?: ""
                            val genreText = subject.genre?.split(",")?.firstOrNull()?.trim() ?: ""
                            val meta = when {
                                releaseText.isNotBlank() && genreText.isNotBlank() -> "$releaseText • $genreText"
                                releaseText.isNotBlank() -> releaseText
                                else -> genreText
                            }
                            if (meta.isNotBlank()) {
                                Text(
                                    text = meta,
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SportsLiveSection(
    section: HomeSection,
    onMatchClick: (LiveMatch) -> Unit
) {
    val matches = section.liveList
    if (matches.isEmpty()) return

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            // Live indicator
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NETFLIX_RED)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIVE NOW",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(matches) { match ->
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DEEP_GREY)
                        .clickable { onMatchClick(match) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FIFA World Cup",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val isHot = match.status == "MatchIng"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isHot) Color.Red else Color.DarkGray)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isHot) "LIVE" else "NOT STARTED",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Score Grid row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Team 1
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(match.team1?.avatar)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = match.team1?.name ?: "Team A",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Score text
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${match.team1?.score ?: "0"} - ${match.team2?.score ?: "0"}",
                                    color = CRUNCHY_ORANGE,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // Team 2
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = match.team2?.name ?: "Team B",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(match.team2?.avatar)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Start details description
                        match.content?.let { desc ->
                            Text(
                                text = desc.replace("\n", " "),
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 10.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingRow(
    historyItems: List<WatchHistoryItem>,
    onItemClick: (WatchHistoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Continue Watching",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(historyItems) { item ->
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DEEP_GREY)
                        .clickable { onItemClick(item) }
                ) {
                    Column {
                        // Poster banner thumbnail
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.posterPath)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Play Overlay Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .align(Alignment.Center),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Dynamic Progress Bar (M3 matching)
                            val progress = if (item.durationMs > 0) {
                                item.lastPositionMs.toFloat() / item.durationMs.toFloat()
                            } else {
                                0f
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Color.Gray.copy(alpha = 0.4f))
                                    .align(Alignment.BottomStart)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                                        .background(CRUNCHY_ORANGE)
                                )
                            }
                        }

                        // Info details
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val episodeText = if (item.lastEpisodeNumber != null) {
                                "S1:E${item.lastEpisodeNumber} - ${item.lastEpisodeTitle}"
                            } else {
                                "Movie Progress"
                            }
                            Text(
                                text = episodeText,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
