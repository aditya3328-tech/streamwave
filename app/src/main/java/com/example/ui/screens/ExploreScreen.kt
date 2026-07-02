package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.CRUNCHY_ORANGE
import com.example.ui.components.MovieCard
import com.example.ui.viewmodel.CineVerseViewModel

// MovieBox-style palette for the Explore experience.
private val EXPLORE_BG = Color(0xFF071A2E)
private val EXPLORE_BUTTON = Color(0xFF3E5B7C)

// Filter option lists (MovieBox dataset).
private val EXPLORE_GENRES = listOf(
    "All", "Action", "Adventure", "Animation", "Biography", "Comedy", "Crime",
    "Documentary", "Drama", "Family", "Fantasy", "Film-Noir", "Game-Show", "History",
    "Horror", "Music", "Musical", "Mystery", "News", "Reality-TV", "Romance", "Sci-Fi",
    "Short", "Sport", "Talk-Show", "Thriller", "War", "Western", "Other"
)

private val EXPLORE_COUNTRIES = listOf(
    "All", "United States", "United Kingdom", "Korea", "Japan", "Bangladesh", "China",
    "Egypt", "France", "Germany", "India", "Indonesia", "Iraq", "Italy", "Ivory Coast",
    "Kenya", "Lebanon", "Mexico", "Morocco", "Nigeria", "Pakistan", "Philippines",
    "Russia", "Saudi Arabia", "South Africa", "Spain", "Syria", "Thailand", "Malaysia",
    "Turkey", "Other"
)

private val EXPLORE_YEARS = listOf(
    "All", "2026", "2025", "2024", "2023", "2022", "2021", "2020",
    "2010s", "2000s", "1990s", "1980s", "Other"
)

@Composable
fun ExploreScreen(
    viewModel: CineVerseViewModel,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items by viewModel.exploreList.collectAsState()
    val isLoading by viewModel.isExploreLoading.collectAsState()
    val isLoadingMore by viewModel.isExploreLoadingMore.collectAsState()
    val hasMore by viewModel.exploreHasMore.collectAsState()

    val selectedGenre by viewModel.exploreSelectedGenre.collectAsState()
    val selectedCountry by viewModel.exploreSelectedCountry.collectAsState()
    val selectedYear by viewModel.exploreSelectedYear.collectAsState()

    // Which full-screen filter modal is currently open (null = none).
    var activeFilter by remember { mutableStateOf<String?>(null) }

    val gridState = rememberLazyGridState()

    // Infinite scroll: trigger the next page when nearing the end of the grid.
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 6
        }
    }
    LaunchedEffect(shouldLoadMore, hasMore) {
        if (shouldLoadMore && hasMore) {
            viewModel.loadMoreExplore()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EXPLORE_BG)
            .statusBarsPadding()
    ) {
        // Screen title
        Text(
            text = "Explore",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Filter button row: [Genre ▼] [Year ▼] [Country ▼] [Filter Icon]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExploreFilterButton(
                label = if (selectedGenre == "All") "Genre" else selectedGenre,
                onClick = { activeFilter = "Genre" },
                modifier = Modifier.weight(1f)
            )
            ExploreFilterButton(
                label = if (selectedYear == "All") "Year" else selectedYear,
                onClick = { activeFilter = "Year" },
                modifier = Modifier.weight(1f)
            )
            ExploreFilterButton(
                label = if (selectedCountry == "All") "Country" else selectedCountry,
                onClick = { activeFilter = "Country" },
                modifier = Modifier.weight(1f)
            )
            // Reset filters
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(EXPLORE_BUTTON)
                    .clickable { viewModel.resetExploreFilters() }
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Reset filters",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            items.isEmpty() -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No results found",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No titles match the selected genre, year or country. Try adjusting your filters.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items, key = { it.id }) { media ->
                        MovieCard(
                            media = media,
                            onClick = { onNavigateToDetails(media.id) },
                            modifier = Modifier.testTag("explore_item_card")
                        )
                    }

                    if (isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Full-screen MovieBox-style filter modal.
    when (activeFilter) {
        "Genre" -> ExploreFilterModal(
            title = "Genre",
            options = EXPLORE_GENRES,
            selected = selectedGenre,
            onSelect = {
                viewModel.applyExploreFilters(it, selectedCountry, selectedYear)
                activeFilter = null
            },
            onDismiss = { activeFilter = null }
        )
        "Year" -> ExploreFilterModal(
            title = "Year",
            options = EXPLORE_YEARS,
            selected = selectedYear,
            onSelect = {
                viewModel.applyExploreFilters(selectedGenre, selectedCountry, it)
                activeFilter = null
            },
            onDismiss = { activeFilter = null }
        )
        "Country" -> ExploreFilterModal(
            title = "Country",
            options = EXPLORE_COUNTRIES,
            selected = selectedCountry,
            onSelect = {
                viewModel.applyExploreFilters(selectedGenre, it, selectedYear)
                activeFilter = null
            },
            onDismiss = { activeFilter = null }
        )
    }
}

@Composable
private fun ExploreFilterButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(EXPLORE_BUTTON)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "▼", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
        }
    }
}

// Full-screen modal sheet (MovieBox style): [X] top-right, large title, vertical option list.
@Composable
private fun ExploreFilterModal(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EXPLORE_BG)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Close button (top-right)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onDismiss() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Large title
            Text(
                text = title,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Vertical option list
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(options, key = { it }) { option ->
                    val isSelected = option == selected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { onSelect(option) }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = option,
                            color = if (isSelected) EXPLORE_BG else Color.White,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
