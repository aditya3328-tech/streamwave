package com.example.sports.player

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sports.model.Match
import com.example.sports.model.PlaySource
import com.example.sports.ui.SportsAccent
import com.example.sports.viewmodel.MatchDetailsViewModel
import com.example.sports.viewmodel.SportsUiState
import com.example.sports.viewmodel.SportsViewModels

/**
 * Full-screen live player for a match. Loads the fixture, then renders the standalone
 * [SportsPlayer] with the [SportsPlayerControls] scoreboard overlay and channel selector.
 *
 * Clicking a channel chip swaps the active stream ([PlaySource.url]); the player re-prepares.
 */
@Composable
fun SportsPlayerScreen(
    matchId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val vm: MatchDetailsViewModel = viewModel(factory = SportsViewModels.factory)
    val state by vm.match.collectAsState()

    LaunchedEffect(matchId) { vm.load(matchId) }

    // Keep the screen awake while live.
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    BackHandler(onBack = onBack)

    Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is SportsUiState.Loading -> CircularProgressIndicator(color = SportsAccent)
            is SportsUiState.Error -> Text(s.message, color = Color.White, fontSize = 14.sp)
            is SportsUiState.Success -> LivePlayerContent(match = s.data, onBack = onBack)
        }
    }
}

@Composable
private fun LivePlayerContent(match: Match, onBack: () -> Unit) {
    // Build the channel list: real play sources, or a single channel from playPath.
    val sources = remember(match) {
        match.playSource.ifEmpty {
            if (match.playPath.isNotBlank()) listOf(PlaySource("Channel 1", match.playPath)) else emptyList()
        }
    }
    var selectedChannel by rememberSaveable(match.id) { mutableIntStateOf(0) }
    val streamUrl = sources.getOrNull(selectedChannel)?.url
        ?: match.playPath

    Box(modifier = Modifier.fillMaxSize()) {
        SportsPlayer(
            streamUrl = streamUrl,
            modifier = Modifier.fillMaxSize(),
        )
        SportsPlayerControls(
            match = match,
            sources = sources,
            selectedChannel = selectedChannel,
            onSelectChannel = { selectedChannel = it },
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Compact, embeddable live player used at the top of the match-details screen (16:9), without
 * the full overlay. Tapping it is handled by the caller (to open the full [SportsPlayerScreen]).
 */
@Composable
fun SportsLivePlayerHeader(streamUrl: String, modifier: Modifier = Modifier) {
    SportsPlayer(
        streamUrl = streamUrl,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    )
}
