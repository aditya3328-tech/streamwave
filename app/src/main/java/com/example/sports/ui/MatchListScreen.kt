package com.example.sports.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sports.viewmodel.SportsHomeViewModel
import com.example.sports.viewmodel.SportsUiState

/**
 * "Matches" top tab — a flat, scrollable list of full [MatchCard]s. Reuses the shared
 * [SportsHomeViewModel] so chip filtering on Home stays in sync with this list.
 */
@Composable
fun MatchListScreen(
    vm: SportsHomeViewModel,
    onMatchClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.matches.collectAsState()

    when (val s = state) {
        is SportsUiState.Loading -> CenterLoader()
        is SportsUiState.Error -> SportsMessage("Couldn't load matches. ${s.message}")
        is SportsUiState.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            ) {
                items(s.data, key = { it.id }) { match ->
                    MatchCard(
                        match = match,
                        onClick = { onMatchClick(match.id) },
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}
