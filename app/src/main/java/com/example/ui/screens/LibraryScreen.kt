package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MediaSummary
import com.example.ui.components.*
import com.example.ui.viewmodel.CineVerseViewModel

@Composable
fun LibraryScreen(
    viewModel: CineVerseViewModel,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val watchlist by viewModel.watchlist.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    // Library Tab: 0 for Watchlist, 1 for Favorites
    var localLibraryTab by rememberSaveable { mutableStateOf(0) }

    AMOLEDCanvas(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header Profile Name
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Text(
                        "My CineVerse",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "Your saved cinema collection",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Tabs Selector Header (Watchlist vs Favorites)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DEEP_GREY)
                        .padding(4.dp)
                ) {
                    // Watchlist Tab Trigger
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (localLibraryTab == 0) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { localLibraryTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Watchlist (${watchlist.size})",
                            color = if (localLibraryTab == 0) CRUNCHY_ORANGE else Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Favorites Tab Trigger
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (localLibraryTab == 1) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                            .clickable { localLibraryTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Favorites (${favorites.size})",
                            color = if (localLibraryTab == 1) CRUNCHY_ORANGE else Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Render Watchlist List
            if (localLibraryTab == 0) {
                if (watchlist.isEmpty()) {
                    item {
                        EmptyLibraryPlaceholder(
                            title = "Watchlist is empty",
                            subtitle = "Find and tap '+' on details screens to save them here for later."
                        )
                    }
                } else {
                    items(watchlist, key = { it.mediaId }) { item ->
                        LibraryItemRow(
                            id = item.mediaId,
                            title = item.title,
                            posterPath = item.posterPath,
                            mediaType = item.mediaType,
                            rating = item.rating,
                            year = item.year,
                            onClick = { onNavigateToDetails(item.mediaId) },
                            onDeleteClick = {
                                viewModel.toggleWatchlist(
                                    MediaSummary(item.mediaId, item.title, item.posterPath, "", item.mediaType, item.rating, item.year)
                                )
                            }
                        )
                    }
                }
            } else {
                // Render Favorites List
                if (favorites.isEmpty()) {
                    item {
                        EmptyLibraryPlaceholder(
                            title = "No Favorites yet",
                            subtitle = "Highlight film pieces you love with heart icons on description cards!"
                        )
                    }
                } else {
                    items(favorites, key = { it.mediaId }) { item ->
                        LibraryItemRow(
                            id = item.mediaId,
                            title = item.title,
                            posterPath = item.posterPath,
                            mediaType = item.mediaType,
                            rating = item.rating,
                            year = item.year,
                            onClick = { onNavigateToDetails(item.mediaId) },
                            onDeleteClick = {
                                viewModel.toggleFavorite(
                                    MediaSummary(item.mediaId, item.title, item.posterPath, "", item.mediaType, item.rating, item.year)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryItemRow(
    id: String,
    title: String,
    posterPath: String,
    mediaType: String,
    rating: Double,
    year: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 76.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DEEP_GREY)
        ) {
            OptimizedPosterImage(
                data = posterPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                widthPx = 108,
                heightPx = 152
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title and meta
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${mediaType.uppercase()} • $year • Rating: $rating/10",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }

        // Delete trigger
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove",
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun EmptyLibraryPlaceholder(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
