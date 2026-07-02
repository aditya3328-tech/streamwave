# StreamWave Usage Examples

Complete code examples for integrating StreamWave into your Android app.

## Example 1: Basic ViewModel Setup

```kotlin
package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Caption
import com.example.data.model.Stream
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(private val context: Context) : ViewModel() {
    
    private val streamRepository by lazy { StreamRepository(context) }
    
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState = _uiState.asStateFlow()
    
    private val _currentStream = MutableStateFlow<Stream?>(null)
    val currentStream = _currentStream.asStateFlow()
    
    private val _captions = MutableStateFlow<List<Caption>>(emptyList())
    val captions = _captions.asStateFlow()
    
    fun loadStreams(
        subjectId: String,
        season: Int = 0,
        episode: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            
            try {
                val result = streamRepository.getStreams(
                    subjectId = subjectId,
                    season = season,
                    episode = episode
                )
                
                if (result.success && result.selectedStream != null) {
                    _currentStream.value = result.selectedStream
                    _uiState.value = PlayerUiState.Success(result.streams)
                    
                    // Load captions
                    val captions = streamRepository.getCaptions(
                        id = result.selectedStream.id,
                        subjectId = subjectId
                    )
                    _captions.value = captions
                } else {
                    _uiState.value = PlayerUiState.Error(
                        message = "Failed to load streams after trying all providers"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error(
                    message = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun selectStream(stream: Stream) {
        _currentStream.value = stream
    }
    
    fun selectCaption(caption: Caption) {
        // Handle caption selection
    }
}

sealed class PlayerUiState {
    object Idle : PlayerUiState()
    object Loading : PlayerUiState()
    data class Success(val streams: List<Stream>) : PlayerUiState()
    data class Error(val message: String) : PlayerUiState()
}
```

## Example 2: Player Screen

```kotlin
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.ui.components.StreamWaveVideoPlayer
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.PlayerUiState

@Composable
fun PlayerScreen(
    subjectId: String,
    season: Int = 0,
    episode: Int = 0,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModelFactory(context)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val currentStream by viewModel.currentStream.collectAsState()
    val captions by viewModel.captions.collectAsState()
    
    LaunchedEffect(subjectId) {
        viewModel.loadStreams(subjectId, season, episode)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (uiState) {
            is PlayerUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            
            is PlayerUiState.Success -> {
                val success = uiState as PlayerUiState.Success
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // Video Player
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) {
                        StreamWaveVideoPlayer(
                            streams = success.streams,
                            captions = captions,
                            selectedStream = currentStream,
                            onStreamSelected = { stream ->
                                viewModel.selectStream(stream)
                            },
                            onCaptionSelected = { caption ->
                                viewModel.selectCaption(caption)
                            },
                            onNextEpisode = {
                                viewModel.loadStreams(
                                    subjectId,
                                    season,
                                    episode + 1
                                )
                            },
                            onPreviousEpisode = {
                                viewModel.loadStreams(
                                    subjectId,
                                    season,
                                    (episode - 1).coerceAtLeast(0)
                                )
                            }
                        )
                    }
                    
                    // Video Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Season $season • Episode $episode",
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current: ${currentStream?.quality} ${currentStream?.type?.uppercase()}",
                            color = Color.Gray
                        )
                    }
                }
            }
            
            is PlayerUiState.Error -> {
                val error = uiState as PlayerUiState.Error
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⚠️ Error",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error.message,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
            
            is PlayerUiState.Idle -> {
                // Initial state
            }
        }
    }
}

class PlayerViewModelFactory(private val context: Context) {
    fun create(): PlayerViewModel = PlayerViewModel(context)
}
```

## Example 3: Navigation Integration

```kotlin
package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.DetailScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // Home Screen
        composable("home") {
            HomeScreen(
                onNavigateToDetail = { subjectId ->
                    navController.navigate("detail/$subjectId")
                }
            )
        }
        
        // Detail Screen
        composable(
            route = "detail/{subjectId}",
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            DetailScreen(
                subjectId = subjectId,
                onPlay = { season, episode ->
                    navController.navigate("player/$subjectId?season=$season&episode=$episode")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Player Screen - Main Implementation
        composable(
            route = "player/{subjectId}?season={season}&episode={episode}",
            arguments = listOf(
                navArgument("subjectId") {
                    type = NavType.StringType
                },
                navArgument("season") {
                    type = NavType.IntType
                    defaultValue = 0
                },
                navArgument("episode") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val season = backStackEntry.arguments?.getInt("season") ?: 0
            val episode = backStackEntry.arguments?.getInt("episode") ?: 0
            
            PlayerScreen(
                subjectId = subjectId,
                season = season,
                episode = episode,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

## Example 4: Testing Providers

```kotlin
package com.example.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Test

class ProviderTests {
    
    @Test
    fun testMovieBoxProvider() = runBlocking {
        val response = MovieBoxProvider.getStreams(
            subjectId = "test-123",
            season = 1,
            episode = 1
        )
        
        assert(response.provider == "MovieBox")
        if (response.success) {
            assert(response.streams.isNotEmpty())
            response.streams.forEach { stream ->
                assert(stream.url.isNotEmpty())
                assert(stream.type in listOf("hls", "mp4"))
            }
        }
    }
    
    @Test
    fun testStreamValidation() = runBlocking {
        val testUrls = listOf(
            "https://example.com/stream.m3u8",
            "https://example.com/video.mp4"
        )
        
        testUrls.forEach { url ->
            val type = StreamValidator.detectStreamType(url)
            assert(type in listOf("hls", "mp4", "unknown"))
        }
    }
    
    @Test
    fun testQualityParsing() {
        val testCases = mapOf(
            "1080p" to 1080,
            "720p" to 720,
            "480p" to 480,
            "unknown" to 0
        )
        
        testCases.forEach { (input, expected) ->
            val result = StreamValidator.parseQuality(input)
            assert(result == expected)
        }
    }
}
```

## Example 5: Error Handling

```kotlin
package com.example.ui.screens

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember

@Composable
fun PlayerScreenWithErrorHandling(subjectId: String) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState) {
        if (uiState is PlayerUiState.Error) {
            val error = uiState as PlayerUiState.Error
            snackbarHostState.showSnackbar(
                message = error.message,
                actionLabel = "Retry"
            )
        }
    }
    
    Box {
        // Player content
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
```

## Example 6: Cache Management

```kotlin
package com.example.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: PlayerViewModel) {
    val context = LocalContext.current
    val repository = remember { StreamRepository(context) }
    
    Column {
        Button(
            onClick = {
                viewModel.viewModelScope.launch {
                    repository.clearCache()
                }
            }
        ) {
            Text("Clear Cache")
        }
        
        Button(
            onClick = {
                viewModel.viewModelScope.launch {
                    repository.clearExpiredCache()
                }
            }
        ) {
            Text("Clean Up Expired")
        }
    }
}
```

## Example 7: Multi-Episode Playback

```kotlin
package com.example.ui.viewmodel

class SeriesPlayerViewModel(context: Context) : ViewModel() {
    
    private val repository = StreamRepository(context)
    
    private val _episodes = MutableStateFlow<List<EpisodeInfo>>(emptyList())
    val episodes = _episodes.asStateFlow()
    
    fun loadSeries(subjectId: String, totalEpisodes: Int) {
        viewModelScope.launch {
            val episodes = (1..totalEpisodes).map { ep ->
                EpisodeInfo(
                    number = ep,
                    subjectId = subjectId,
                    status = EpisodeStatus.IDLE
                )
            }
            _episodes.value = episodes
        }
    }
    
    fun playEpisode(episodeNumber: Int, subjectId: String) {
        viewModelScope.launch {
            val result = repository.getStreams(
                subjectId = subjectId,
                season = 1,
                episode = episodeNumber
            )
            
            if (result.success) {
                // Update UI with available streams
            }
        }
    }
}

data class EpisodeInfo(
    val number: Int,
    val subjectId: String,
    val status: EpisodeStatus
)

enum class EpisodeStatus {
    IDLE, LOADING, READY, ERROR
}
```

## Example 8: Provider Debugging

```kotlin
package com.example.util

import android.util.Log
import com.example.data.remote.providers.MovieBoxProvider
import com.example.data.remote.providers.ProviderA
import com.example.data.remote.StreamResolver
import kotlinx.coroutines.runBlocking

fun debugProviders(subjectId: String) {
    runBlocking {
        Log.d("DebugProviders", "=== Testing Providers ===")
        
        // Test MovieBox
        Log.d("DebugProviders", "Testing MovieBox...")
        val movieBoxResult = MovieBoxProvider.getStreams(subjectId)
        Log.d("DebugProviders", "MovieBox: ${movieBoxResult.success} (${movieBoxResult.streams.size} streams)")
        movieBoxResult.streams.forEach { stream ->
            Log.d("DebugProviders", "  - ${stream.quality} ${stream.type} ${stream.url.take(50)}")
        }
        
        // Test ProviderA
        Log.d("DebugProviders", "Testing ProviderA...")
        val providerAResult = ProviderA.getStreams(subjectId)
        Log.d("DebugProviders", "ProviderA: ${providerAResult.success} (${providerAResult.streams.size} streams)")
        providerAResult.streams.forEach { stream ->
            Log.d("DebugProviders", "  - ${stream.quality} ${stream.type}")
        }
        
        Log.d("DebugProviders", "=== Complete ===")
    }
}
```

## Example 9: Custom Provider Implementation

```kotlin
package com.example.data.remote.providers

import com.example.data.model.Caption
import com.example.data.model.ProviderStreamResponse
import com.example.data.model.Stream

class CustomProvider : StreamProvider {
    
    override suspend fun getStreams(
        subjectId: String,
        detailPath: String,
        season: Int,
        episode: Int
    ): ProviderStreamResponse {
        return try {
            // Call your custom API
            val streams = listOf(
                Stream(
                    id = "1",
                    provider = "CustomProvider",
                    url = "https://your-api.com/stream.m3u8",
                    type = "hls",
                    quality = "1080p",
                    numericQuality = 1080
                )
            )
            
            ProviderStreamResponse(
                provider = "CustomProvider",
                success = true,
                streams = streams,
                message = "Success"
            )
        } catch (e: Exception) {
            ProviderStreamResponse(
                provider = "CustomProvider",
                success = false,
                streams = emptyList(),
                message = "Error",
                error = e.message
            )
        }
    }
    
    override suspend fun getCaptions(
        id: String,
        subjectId: String,
        detailPath: String
    ): List<Caption> {
        return emptyList()
    }
}

// Register in ProviderRegistry
object CustomProviderImpl : StreamProvider by CustomProvider()
```

## Example 10: Analytics Integration

```kotlin
package com.example.analytics

import com.example.data.model.ResolvedStreamPayload
import com.example.data.model.Stream

object StreamAnalytics {
    
    fun logStreamResolved(payload: ResolvedStreamPayload) {
        // Log to your analytics service
        val data = mapOf(
            "success" to payload.success,
            "stream_count" to payload.streams.size,
            "selected_quality" to payload.selectedStream?.quality,
            "selected_type" to payload.selectedStream?.type,
            "provider" to payload.selectedStream?.provider,
            "from_cache" to payload.fromCache,
            "attempts" to payload.attempts.size
        )
        logEvent("stream_resolved", data)
    }
    
    fun logStreamPlayback(stream: Stream, position: Long) {
        val data = mapOf(
            "provider" to stream.provider,
            "quality" to stream.quality,
            "type" to stream.type,
            "position_ms" to position
        )
        logEvent("stream_playback", data)
    }
    
    fun logStreamError(error: String, provider: String) {
        val data = mapOf(
            "error" to error,
            "provider" to provider
        )
        logEvent("stream_error", data)
    }
    
    private fun logEvent(name: String, data: Map<String, Any>) {
        // Implement your analytics backend
    }
}
```

---

All examples are ready to use and follow the StreamWave architecture patterns!
