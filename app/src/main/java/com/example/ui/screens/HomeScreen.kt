package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
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

    // Retain scroll position across recompositions/config-changes and feed the (single) vertical
    // LazyColumn an explicit, remembered state.
    val listState = rememberLazyListState()

    // HOME_TRACE diagnostics — section/item counts + heap usage. Home is single-shot (no
    // pagination/loadMore), so this fires only when the section set actually changes.
    LaunchedEffect(homeSections) {
        val totalItems = homeSections.sumOf { s ->
            s.subjects.size + s.liveList.size + s.filters.size +
                (s.banner?.items?.size ?: 0) + (s.customData?.items?.size ?: 0)
        }
        val rt = Runtime.getRuntime()
        val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        val maxMb = rt.maxMemory() / (1024 * 1024)
        android.util.Log.d(
            "HOME_TRACE",
            "sections=${homeSections.size} items=$totalItems banners=${banners.size} " +
                "history=${watchHistory.size} heapUsedMb=$usedMb heapMaxMb=$maxMb"
        )
    }

    AMOLEDCanvas(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp) // safe target for bottom nav bar
        ) {
            // Skeletons when loading and sections are empty (First launch scenario)
            if (isHomeLoading && homeSections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
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

            // CMS Dynamic Renderer.
            // Key MUST be globally unique — the API can return sections sharing the same
            // type+position, which previously produced duplicate keys and an IllegalArgumentException
            // ("Key was already used") the moment the colliding section scrolled into view. The
            // trailing index makes every key unique regardless of the payload.
            itemsIndexed(
                homeSections,
                key = { index, section -> "${section.type}_${section.position}_$index" }
            ) { _, section ->
                when (section.type) {
                    "BANNER" -> {
                        val context = LocalContext.current
                        // Banner first, then Continue Watching directly beneath it (moved here
                        // from the top of the list — same row UI, only the position changed).
                        Column {
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
                            if (watchHistory.isNotEmpty()) {
                                ContinueWatchingRow(
                                    historyItems = watchHistory,
                                    onItemClick = { item ->
                                        onNavigateToPlayer(item.mediaId, item.lastEpisodeId)
                                    }
                                )
                            }
                        }
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

    // Old MovieBox-style hero: full-bleed backdrop + a single floating glass card at the bottom.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)                 // banner height unchanged
            .background(Color.Black)        // solid base so no gray ever shows behind the artwork
            .clickable { onBannerClick(activeItem) }
    ) {
        // Full-width backdrop — edge-to-edge, fully covering the hero.
        // fillMaxSize + Crop guarantees no letterboxing / black bars / gray margins.
        OptimizedPosterImage(
            data = activeItem.image?.url,
            contentDescription = activeItem.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            widthPx = 1280,
            heightPx = 720,
            placeholderColor = Color.Black
        )

        // Subtle overall black overlay for readability.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        // Stronger black gradient toward the bottom, behind the floating card.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Floating glass movie card near the bottom of the banner.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT — small poster (~50x75dp), rounded corners.
            OptimizedPosterImage(
                data = activeItem.subject?.cover?.url ?: activeItem.image?.url,
                contentDescription = activeItem.title,
                modifier = Modifier
                    .size(width = 50.dp, height = 75.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
                widthPx = 150,
                heightPx = 225,
                placeholderColor = DEEP_GREY
            )

            Spacer(modifier = Modifier.width(12.dp))

            // CENTER — title (max 2 lines) + metadata row.
            Column(modifier = Modifier.weight(1f)) {
                // Many AoneRoom banners send an empty top-level "title" while the real name
                // lives in subject.title — fall back to it before showing a placeholder.
                val movieTitle = when {
                    !activeItem.title.isNullOrBlank() -> activeItem.title
                    !activeItem.subject?.title.isNullOrBlank() -> activeItem.subject!!.title
                    else -> "Unknown Title"
                }
                Text(
                    text = movieTitle,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Metadata: 2026 | Action | Adventure
                val metaParts = remember(activeItem) {
                    buildList {
                        activeItem.subject?.releaseDate?.take(4)
                            ?.takeIf { it.isNotBlank() }?.let { add(it) }
                        activeItem.subject?.genre
                            ?.split(",")
                            ?.map { it.trim() }
                            ?.filter { it.isNotEmpty() }
                            ?.take(2)
                            ?.let { addAll(it) }
                    }
                }
                if (metaParts.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = metaParts.joinToString(" | "),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // RIGHT — small circular green play button (icon only).
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .clickable { onBannerClick(activeItem) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
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
            itemsIndexed(
                section.filters,
                key = { index, it -> "${it.title}_${it.query}_$index" }
            ) { _, filter ->
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DEEP_GREY)
                        .clickable { onFilterClick(filter.query ?: filter.title) }
                ) {
                    // Image banner behind filter text
                    OptimizedPosterImage(
                        data = filter.image?.url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        widthPx = 260,
                        heightPx = 144
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
    // Resolve once per section instead of recomputing the mapNotNull on every recomposition.
    val subjects = remember(section) {
        if (section.subjects.isNotEmpty()) {
            section.subjects
        } else {
            section.customData?.items?.mapNotNull { it.subject } ?: emptyList()
        }
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
            // The feed routinely repeats the same subject within a single rail (e.g. a title that
            // is both "Trending" and "For You"). A bare subjectId key therefore collided and crashed
            // with IllegalArgumentException when the duplicate composed on scroll. The index makes
            // every key unique even for genuine duplicate subjects.
            itemsIndexed(
                subjects,
                key = { index, subject -> "${subject.subjectType}_${subject.subjectId}_$index" }
            ) { _, subject ->
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
                            OptimizedPosterImage(
                                data = subject.cover?.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                widthPx = 240,
                                heightPx = 320
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
            itemsIndexed(
                matches,
                key = { index, it -> "${it.matchId}_$index" }
            ) { _, match ->
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
                                OptimizedPosterImage(
                                    data = match.team1?.avatar,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit,
                                    widthPx = 48,
                                    heightPx = 48
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
                                OptimizedPosterImage(
                                    data = match.team2?.avatar,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    contentScale = ContentScale.Fit,
                                    widthPx = 48,
                                    heightPx = 48
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
            items(historyItems, key = { it.mediaId }) { item ->
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
                            OptimizedPosterImage(
                                data = item.posterPath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                widthPx = 400,
                                heightPx = 220
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
