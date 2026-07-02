package com.example.sports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sports.model.SportsSection
import com.example.sports.viewmodel.SportsHomeViewModel
import com.example.sports.viewmodel.SportsUiState
import com.example.sports.viewmodel.SportsViewModels

/** The three top tabs of the Sports section, exactly like the SportsNow UI. */
private enum class SportsTab(val label: String) { HOME("Home"), MATCHES("Matches"), NEWS("News") }

/**
 * Root Sports screen opened by the SPORTS bottom tab. Hosts the Home / Matches / News top tabs
 * and switches their content. Shares NOTHING with the movie home screen.
 */
@Composable
fun SportsHomeScreen(
    onMatchClick: (String) -> Unit,
    onArticleClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(SportsTab.HOME) }
    val homeVm: SportsHomeViewModel = viewModel(factory = SportsViewModels.factory)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SportsBg)
            .statusBarsPadding(),
    ) {
        // ---- App bar ----
        Text(
            text = "Sports",
            color = SportsText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
        )

        // ---- Top tabs ----
        SportsTopTabs(selected = tab, onSelect = { tab = it })

        when (tab) {
            SportsTab.HOME -> SportsHomeTab(vm = homeVm, onMatchClick = onMatchClick)
            SportsTab.MATCHES -> MatchListScreen(vm = homeVm, onMatchClick = onMatchClick)
            SportsTab.NEWS -> SportsNewsScreen(onArticleClick = onArticleClick)
        }
    }
}

@Composable
private fun SportsTopTabs(selected: SportsTab, onSelect: (SportsTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SportsTab.entries.forEach { t ->
            val isSel = t == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) SportsCard else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(
                    text = t.label,
                    color = if (isSel) SportsAccent else SportsTextDim,
                    fontSize = 15.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** "Home" tab: filter chips + the titled sections (Trending leagues, FIFA World Cup, …). */
@Composable
private fun SportsHomeTab(vm: SportsHomeViewModel, onMatchClick: (String) -> Unit) {
    val state by vm.home.collectAsState()
    val selectedChip by vm.selectedChip.collectAsState()

    when (val s = state) {
        is SportsUiState.Loading -> CenterLoader()
        is SportsUiState.Error -> SportsMessage("Couldn't load sports. ${s.message}")
        is SportsUiState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // Horizontal chips: All / FIFA World Cup / Premier League / More ▼
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(vm.chips) { chip ->
                            SportsChip(
                                label = chip.label,
                                selected = chip.id == selectedChip,
                                onClick = { vm.selectChip(chip.id) },
                            )
                        }
                    }
                }

                s.data.forEachIndexed { index, section ->
                    item(key = "header_$index") { SportsSectionHeader(section.title) }
                    if (index == 0) {
                        // First section ("Trending leagues") as a horizontal rail.
                        item(key = "rail_$index") { TrendingRail(section, onMatchClick) }
                    } else {
                        items(section.matches, key = { it.id }) { match ->
                            MatchCard(
                                match = match,
                                onClick = { onMatchClick(match.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingRail(section: SportsSection, onMatchClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(section.matches, key = { it.id }) { match ->
            MatchMiniCard(match = match, onClick = { onMatchClick(match.id) })
        }
    }
}

@Composable
internal fun CenterLoader() {
    Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SportsAccent)
    }
}
