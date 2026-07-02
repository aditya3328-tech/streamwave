package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.MediaSummary
import com.example.data.model.RelatedCelebrity
import com.example.data.model.StaffPageData
import com.example.data.model.StaffProfileInfo
import com.example.ui.components.NETFLIX_RED
import com.example.ui.components.OptimizedPosterImage
import com.example.ui.viewmodel.CineVerseViewModel
import com.example.ui.viewmodel.StaffUiState

private val ProfileBg = Color(0xFF0B1020)
private val CardNavy = Color(0xFF161E33)
private val Accent = NETFLIX_RED
private val SecondaryText = Color(0xFFB0B8C4)

@Composable
fun StaffScreen(
    staffId: String,
    viewModel: CineVerseViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToStaff: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(staffId) {
        viewModel.loadStaffDetails(staffId)
    }

    val staffState by viewModel.staffUiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProfileBg)
    ) {
        when (val state = staffState) {
            is StaffUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }

            is StaffUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = Color.White)
                }
            }

            is StaffUiState.Success -> {
                StaffContent(
                    data = state.data,
                    onNavigateBack = onNavigateBack,
                    onNavigateToDetails = onNavigateToDetails,
                    onNavigateToStaff = onNavigateToStaff,
                    onLoadMore = { viewModel.loadMoreStaffFilmography() }
                )
            }
        }
    }
}

@Composable
private fun StaffContent(
    data: StaffPageData,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToStaff: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    val gridState = rememberLazyGridState()

    // Infinite scrolling: trigger near the end of the grid.
    LaunchedEffect(gridState, data.hasMore) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && data.hasMore && !data.isLoadingMore && last >= total - 4) {
                onLoadMore()
            }
        }
    }

    val fullSpan: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan =
        { GridItemSpan(maxLineSpan) }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = fullSpan) {
            TopBar(onNavigateBack = onNavigateBack)
        }

        item(span = fullSpan) {
            ProfileHeader(profile = data.profile, totalTitles = data.totalTitles)
        }

        if (data.filmography.isNotEmpty()) {
            item(span = fullSpan) {
                SectionTitle(text = "Filmography")
            }
            items(items = data.filmography, key = { it.id }) { movie ->
                FilmographyCard(movie = movie, onClick = { onNavigateToDetails(movie.id) })
            }
        }

        if (data.isLoadingMore) {
            item(span = fullSpan) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Accent, strokeWidth = 2.5.dp)
                }
            }
        }

        if (data.related.isNotEmpty()) {
            item(span = fullSpan) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionTitle(text = "Related Celebrities")
                    Spacer(modifier = Modifier.height(14.dp))
                    RelatedCelebritiesRow(
                        related = data.related,
                        onClick = onNavigateToStaff
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun ProfileHeader(profile: StaffProfileInfo, totalTitles: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(profile.id) { visible = true }
    val imageAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "avatarFade"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircleAvatar(
            url = profile.avatarUrl,
            size = 140.dp,
            modifier = Modifier.alpha(imageAlpha)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = profile.name.ifBlank { "Profile" },
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black
        )

        if (profile.roles.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = profile.roles,
                color = Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (totalTitles > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$totalTitles Titles",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (profile.biography.isNotBlank()) {
            Spacer(modifier = Modifier.height(18.dp))
            ExpandableBiography(text = profile.biography)
        }
    }
}

@Composable
private fun ExpandableBiography(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = SecondaryText,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (expanded) "Show Less" else "Read More",
            color = Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { expanded = !expanded }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun FilmographyCard(movie: MediaSummary, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Lightweight press feedback only — scale read deferred into the layer (no
            // recomposition per frame). Heavy per-card elevation shadow removed for scroll perf.
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(CardNavy)
            .aspectRatio(2f / 3f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        OptimizedPosterImage(
            data = movie.posterPath,
            contentDescription = movie.title,
            modifier = Modifier.fillMaxSize()
        )

        // Rating badge (top-right)
        if (movie.rating > 0.0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = formatRating(movie.rating),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom gradient overlay with title / rating / year
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (movie.rating > 0.0) {
                    Text(
                        text = "${formatRating(movie.rating)} ★",
                        color = Accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (movie.year.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
                if (movie.year.isNotBlank()) {
                    Text(
                        text = movie.year,
                        color = SecondaryText,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RelatedCelebritiesRow(
    related: List<RelatedCelebrity>,
    onClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(items = related, key = { it.id }) { celeb ->
            Column(
                modifier = Modifier
                    .width(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick(celeb.id) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircleAvatar(url = celeb.avatarUrl, size = 72.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = celeb.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (celeb.role.isNotBlank()) {
                    Text(
                        text = celeb.role,
                        color = SecondaryText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleAvatar(
    url: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(CardNavy),
        contentAlignment = Alignment.Center
    ) {
        if (url.isBlank()) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = SecondaryText,
                modifier = Modifier.size(size * 0.5f)
            )
        } else {
            OptimizedPosterImage(
                data = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                widthPx = 150,
                heightPx = 150
            )
        }
    }
}

private fun formatRating(rating: Double): String {
    val rounded = (rating * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
