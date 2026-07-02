package com.example.ui.screens.movie

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AMOLEDCanvas
import com.example.ui.components.CRUNCHY_ORANGE
import com.example.ui.components.SkeletonShimmer
import com.example.ui.screens.ContinueWatchingRow
import com.example.ui.screens.DynamicHeroCarousel
import com.example.ui.screens.FilterSection
import com.example.ui.screens.SubjectsMovieSection
import com.example.ui.viewmodel.MovieTabViewModel

@Composable
fun MovieTabScreen(
    viewModel: MovieTabViewModel,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToPlayer: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading by viewModel.isMovieTabLoading.collectAsState()
    val sections by viewModel.movieTabSections.collectAsState()
    val banners by viewModel.movieTabBanners.collectAsState()
    val error by viewModel.movieTabError.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadMovieTabData()
    }

    AMOLEDCanvas(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
        ) {
            if (isLoading && sections.isEmpty() && banners.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = CRUNCHY_ORANGE)
                    }
                }
                items(3) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SkeletonShimmer(width = 180.dp, height = 24.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                            repeat(3) {
                                SkeletonShimmer(width = 120.dp, height = 160.dp)
                            }
                        }
                    }
                }
            }

            if (!isLoading && sections.isEmpty() && banners.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 100.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Connection Timed Out",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "Could not retrieve live video feed from server. Please verify your connection status and snap retry.",
                            color = androidx.compose.ui.graphics.Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadMovieTabData(forceRefresh = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = CRUNCHY_ORANGE),
                        ) {
                            Text("Retry Connection", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (banners.isNotEmpty()) {
                item(key = "movie_tab_hero") {
                    Column {
                        DynamicHeroCarousel(
                            banners = banners,
                            onBannerClick = { banner ->
                                val subjectId = banner.subjectId ?: "0"
                                if (subjectId != "0" && subjectId.isNotBlank()) {
                                    onNavigateToDetails(subjectId)
                                } else {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(banner.url.orEmpty())),
                                        )
                                    }.onFailure {
                                        Log.e("MOVIE_TAB", "Error launching external url: ${banner.url}", it)
                                    }
                                }
                            },
                        )
                        if (watchHistory.isNotEmpty()) {
                            ContinueWatchingRow(
                                historyItems = watchHistory,
                                onItemClick = { item ->
                                    onNavigateToPlayer(item.mediaId, item.lastEpisodeId)
                                },
                            )
                        }
                    }
                }
            }

            itemsIndexed(
                sections,
                key = { index, section -> "movie_tab_${section.type}_${section.position}_$index" },
            ) { _, section ->
                when (section.type) {
                    "FILTER" -> FilterSection(
                        section = section,
                        onFilterClick = { query ->
                            Log.d("MOVIE_TAB", "Category selected: $query")
                        },
                    )
                    "SUBJECTS_MOVIE" -> SubjectsMovieSection(
                        section = section,
                        onItemClick = onNavigateToDetails,
                    )
                }
            }
        }
    }
}
