package com.example.sports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.sports.model.Highlight
import com.example.sports.model.Match
import com.example.sports.model.News
import com.example.sports.player.SportsLivePlayerHeader
import com.example.sports.viewmodel.MatchDetailsViewModel
import com.example.sports.viewmodel.SportsUiState
import com.example.sports.viewmodel.SportsViewModels

/**
 * Brand-new match-details screen — NOT the movie detail page. Sections (per spec):
 *   1. Large live player at top
 *   2. Team comparison card
 *   3. Match status
 *   4. Match round
 *   5. Highlights carousel (match.highlights[])
 *   6. News section (newsList[])
 *   7. Odds section (match.oddsInfo)
 */
@Composable
fun MatchDetailsScreen(
    matchId: String,
    onBack: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: MatchDetailsViewModel = viewModel(factory = SportsViewModels.factory)
    val state by vm.match.collectAsState()
    val news by vm.news.collectAsState()

    androidx.compose.runtime.LaunchedEffect(matchId) { vm.load(matchId) }

    Box(modifier = modifier.fillMaxSize().background(SportsBg)) {
        when (val s = state) {
            is SportsUiState.Loading -> CenterLoader()
            is SportsUiState.Error -> SportsMessage("Couldn't load match. ${s.message}")
            is SportsUiState.Success -> MatchDetailsContent(
                match = s.data,
                news = news,
                onBack = onBack,
                onOpenPlayer = onOpenPlayer,
                onArticleClick = onArticleClick,
            )
        }
    }
}

@Composable
private fun MatchDetailsContent(
    match: Match,
    news: List<News>,
    onBack: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    onArticleClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        // ---- 1. Large live player at top ----
        item {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                if (match.playPath.isNotBlank() || match.playSource.isNotEmpty()) {
                    SportsLivePlayerHeader(
                        streamUrl = match.playSource.firstOrNull()?.url ?: match.playPath,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Tap target -> full-screen player
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onOpenPlayer(match.id) },
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(28.dp)
                        .clickable(onClick = onBack),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { onOpenPlayer(match.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }
        }

        // ---- 2. Team comparison card + 3. status + 4. round ----
        item { TeamComparisonCard(match) }

        // ---- 5. Highlights carousel ----
        if (match.highlights.isNotEmpty()) {
            item { SportsSectionHeader("Highlights") }
            item { HighlightsCarousel(match.highlights) }
        }

        // ---- 6. News section ----
        if (news.isNotEmpty()) {
            item { SportsSectionHeader("News") }
            items(news, key = { it.id }) { article ->
                MatchNewsRow(article, onClick = { onArticleClick(article.id) })
            }
        }

        // ---- 7. Odds section ----
        if (match.oddsInfo.oddsList.isNotEmpty()) {
            item { SportsSectionHeader("Odds") }
            item { OddsSection(match) }
        }
    }
}

@Composable
private fun TeamComparisonCard(match: Match) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SportsCard)
            .padding(18.dp),
    ) {
        // Status + round
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            MatchStatusBadge(match.status, match.matchTime)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = match.matchRound,
            color = SportsTextDim,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        // logo  0 - 0  logo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComparisonTeam(name = match.team1.name, logo = match.team1.avatar, modifier = Modifier.weight(1f))
            Text(
                text = "${match.team1.score} - ${match.team2.score}",
                color = SportsText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            ComparisonTeam(name = match.team2.name, logo = match.team2.avatar, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        FansSupportBar(home = match.homeSupport, away = match.awaySupport)
    }
}

@Composable
private fun ComparisonTeam(name: String, logo: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(SportsCardAlt),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = logo,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(CircleShape),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            color = SportsText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HighlightsCarousel(highlights: List<Highlight>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(highlights, key = { it.id }) { h ->
            Column(modifier = Modifier.width(220.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SportsCardAlt),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = h.cover.url,
                        contentDescription = h.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    )
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    if (h.duration.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(h.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    h.title, color = SportsText, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MatchNewsRow(news: News, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SportsCard)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        AsyncImage(
            model = news.cover.url,
            contentDescription = news.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 110.dp, height = 72.dp).clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                news.title, color = SportsText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text("${news.author} • ${news.date}", color = SportsTextDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun OddsSection(match: Match) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SportsCard)
            .padding(16.dp),
    ) {
        if (match.oddsInfo.provider.isNotBlank()) {
            Text(match.oddsInfo.provider, color = SportsTextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            match.oddsInfo.oddsList.forEach { odd ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SportsCardAlt)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(odd.label, color = SportsTextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(odd.value, color = SportsAccent, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SportsAccent)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Bet Now", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
