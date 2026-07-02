# StreamWave Integration Guide

Quick setup to integrate StreamWave into your Android app.

## Step 1: Update Dependencies ✅ DONE

All required dependencies have been added to `gradle/libs.versions.toml` and `app/build.gradle.kts`:
- Retrofit + OkHttp
- Room Database
- ExoPlayer (Media3)
- GSON + Moshi
- Coroutines

Run sync to download dependencies.

## Step 2: Configure API URLs

Edit `app/src/main/java/com/example/data/remote/ApiClient.kt`:

```kotlin
object ApiClient {
    private const val PROD_API_BASE = "https://your-api.com/api"  // ← Change this
    private const val DEV_API_BASE = "http://localhost:3000/api"  // ← Change this
    private const val TIMEOUT = 15000L
}
```

## Step 3: Configure Providers

### MovieBox Provider

Edit `app/src/main/java/com/example/data/remote/providers/MovieBoxProvider.kt`:

```kotlin
object MovieBoxProvider {
    private const val API_BASE = "https://moviebox-api.example.com"  // ← Change this
}
```

### ProviderA

Set environment variable or edit `ProviderA.kt`:

```kotlin
// Option 1: Environment variable (recommended)
PROVIDER_A_API_BASE=https://provider-a-api.com

// Option 2: Edit directly in ProviderA.kt
private const val API_BASE = "https://provider-a-api.com"
```

## Step 4: Create ViewModel

Create `app/src/main/java/com/example/ui/viewmodel/PlayerViewModel.kt`:

```kotlin
package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Stream
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    private val streamRepository = StreamRepository(context) // Pass context from Activity
    
    private val _streams = MutableStateFlow<List<Stream>>(emptyList())
    val streams = _streams.asStateFlow()
    
    private val _selectedStream = MutableStateFlow<Stream?>(null)
    val selectedStream = _selectedStream.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    fun loadStreams(subjectId: String, season: Int = 0, episode: Int = 0) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = streamRepository.getStreams(
                    subjectId = subjectId,
                    season = season,
                    episode = episode
                )
                
                if (result.success) {
                    _streams.value = result.streams
                    _selectedStream.value = result.selectedStream
                    _error.value = null
                } else {
                    _error.value = "Failed to load streams from all providers"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectStream(stream: Stream) {
        _selectedStream.value = stream
    }
}
```

## Step 5: Create Player Screen

Create `app/src/main/java/com/example/ui/screens/PlayerScreen.kt`:

```kotlin
package com.example.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.ui.components.StreamWaveVideoPlayer
import com.example.ui.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(
    subjectId: String,
    season: Int = 0,
    episode: Int = 0,
    viewModel: PlayerViewModel = viewModel()
) {
    val streams by viewModel.streams.collectAsState()
    val selectedStream by viewModel.selectedStream.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    LaunchedEffect(subjectId) {
        viewModel.loadStreams(subjectId, season, episode)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            streams.isNotEmpty() -> {
                StreamWaveVideoPlayer(
                    streams = streams,
                    selectedStream = selectedStream,
                    onStreamSelected = { stream ->
                        viewModel.selectStream(stream)
                    },
                    onNextEpisode = {
                        viewModel.loadStreams(subjectId, season, episode + 1)
                    },
                    onPreviousEpisode = {
                        viewModel.loadStreams(subjectId, season, (episode - 1).coerceAtLeast(0))
                    }
                )
            }
            else -> {
                Text(
                    text = "No streams available",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
```

## Step 6: Use in Navigation

Add to your nav graph or screen list:

```kotlin
@Composable
fun AppNavigation() {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { HomeScreen() }
        
        composable(
            route = "player/{subjectId}?season={season}&episode={episode}",
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
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
            
            PlayerScreen(subjectId, season, episode)
        }
    }
}
```

## Step 7: Test Configuration

### Test API Connectivity

Add to your MainActivity or test screen:

```kotlin
@Composable
fun TestStreamButton() {
    val context = LocalContext.current
    
    Button(
        onClick = {
            runBlocking {
                val repository = StreamRepository(context)
                val result = repository.getStreams(
                    subjectId = "test-subject-id",
                    season = 1,
                    episode = 1
                )
                Log.d("StreamTest", "Success: ${result.success}")
                Log.d("StreamTest", "Streams: ${result.streams.size}")
                Log.d("StreamTest", "Selected: ${result.selectedStream?.quality}")
            }
        }
    ) {
        Text("Test Streams")
    }
}
```

## Step 8: Add Logging

Control log levels in `ApiClient.kt`:

```kotlin
private const val LOG_LEVEL = "debug"  // Options: "debug", "info", "warn", "error"
```

View logs in Logcat with tag: `StreamWave`

## Step 9: Handle Errors

Add error handling UI:

```kotlin
if (error != null) {
    Snackbar(
        message = error,
        actionLabel = "Retry",
        onActionPerformed = {
            viewModel.loadStreams(subjectId, season, episode)
        }
    )
}
```

## Step 10: Cache Management (Optional)

Clear cache when needed:

```kotlin
Button(
    onClick = {
        viewModelScope.launch {
            streamRepository.clearCache()
        }
    }
) {
    Text("Clear Cache")
}

// Or clear expired only
Button(
    onClick = {
        viewModelScope.launch {
            streamRepository.clearExpiredCache()
        }
    }
) {
    Text("Clean Up Cache")
}
```

## Troubleshooting

### "Unable to resolve host"
- Check API URLs in `ApiClient.kt` and provider classes
- Verify internet permission in AndroidManifest.xml
- Check firewall/proxy settings

### Streams not validating
- Check stream URLs are accessible
- Verify content-type headers match whitelist in `StreamValidator.kt`
- Check 8-second timeout isn't too short

### Cache not working
- Ensure Room database is properly initialized
- Check file permissions for database
- Try `clearCache()` to reset

### ExoPlayer errors
- Check stream URL format (HLS vs MP4)
- Verify content-type matches stream type
- Check playback permissions

## Performance Tips

1. **Increase cache TTL** for frequently watched content:
   ```kotlin
   const val CACHE_TTL_MS = 7200000L  // 2 hours
   ```

2. **Reduce validation timeout** if network is fast:
   ```kotlin
   val httpClient = OkHttpClient.Builder()
       .connectTimeout(5, TimeUnit.SECONDS)  // was 8
   ```

3. **Parallel provider attempts** (already implemented in StreamResolver)

4. **Preload next episode** while watching current:
   ```kotlin
   LaunchedEffect(currentEpisode) {
       viewModel.preloadStreams(subjectId, season, episode + 1)
   }
   ```

## Security Best Practices

1. **Move Bearer tokens to secure storage**:
   ```kotlin
   // Use Android Keystore or EncryptedSharedPreferences
   val encryptedToken = getFromSecureStorage("api_token")
   ```

2. **Use HTTPS for all APIs** ✅ Already enforced

3. **Enable certificate pinning** (optional):
   ```kotlin
   val certificatePinner = CertificatePinner.Builder()
       .add("api.example.com", "sha256/...")
       .build()
   ```

4. **Validate stream URLs before playback** ✅ Already implemented

## Advanced: Dependency Injection with Hilt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object StreamingModule {
    @Provides
    @Singleton
    fun provideStreamRepository(context: Context): StreamRepository {
        return StreamRepository(context)
    }
}

// In ViewModel
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: StreamRepository
) : ViewModel() {
    // ... rest of code
}

// In Activity
@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {
    // Hilt injects automatically
}
```

## Completed Files

All implementation files are ready to use:

✅ `ApiClient.kt` - HTTP client with logging
✅ `ApiEndpoints.kt` - API routes
✅ `StreamModels.kt` - Data classes
✅ `StreamValidator.kt` - Stream validation
✅ `MovieBoxProvider.kt` - Provider adapter
✅ `ProviderA.kt` - Alternative provider
✅ `StreamProvider.kt` - Interface + registry
✅ `StreamCache.kt` - Caching layer
✅ `StreamWaveDatabase.kt` - Room database
✅ `StreamResolver.kt` - Core orchestration
✅ `StreamRepository.kt` - High-level API
✅ `StreamWaveVideoPlayer.kt` - Compose player
✅ `STREAMWAVE_ARCHITECTURE.md` - Full documentation

## Next: Usage Examples

See individual file headers for detailed usage examples and JavaDoc comments.

All components follow the same patterns as the web reference implementation!
