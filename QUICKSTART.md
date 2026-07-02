# 🚀 StreamWave Quick Reference

Copy-paste snippets for rapid integration.

## 1️⃣ Basic Setup - Load Streams

```kotlin
// In your Activity or ViewModel
val repository = StreamRepository(context)

// Load streams with fallback
val result = repository.getStreams(
    subjectId = "123",
    season = 1,
    episode = 5
)

// Result contains:
result.success          // Boolean - all streams valid?
result.streams          // List<Stream> - all options
result.selectedStream   // Stream? - best quality auto-selected
result.fromCache        // Boolean - was this cached?
```

## 2️⃣ Display Player

```kotlin
// In your Composable
StreamWaveVideoPlayer(
    streams = result.streams,
    selectedStream = result.selectedStream,
    captions = captions,
    onStreamSelected = { stream -> /* quality changed */ },
    onCaptionSelected = { caption -> /* subtitle changed */ },
    onNextEpisode = { viewModel.loadStreams(subjectId, season, episode+1) },
    onPreviousEpisode = { viewModel.loadStreams(subjectId, season, episode-1) }
)
```

## 3️⃣ ViewModel Template

```kotlin
class PlayerViewModel(context: Context) : ViewModel() {
    
    private val repository = StreamRepository(context)
    
    private val _streams = MutableStateFlow<List<Stream>>(emptyList())
    val streams = _streams.asStateFlow()
    
    fun loadStreams(subjectId: String, season: Int, episode: Int) {
        viewModelScope.launch {
            try {
                val result = repository.getStreams(subjectId, season, episode)
                _streams.value = result.streams
            } catch (e: Exception) {
                Log.e("Player", "Error: ${e.message}")
            }
        }
    }
}
```

## 4️⃣ Navigation Setup

```kotlin
composable(
    route = "player/{subjectId}?season={season}&episode={episode}",
    arguments = listOf(
        navArgument("subjectId") { type = NavType.StringType },
        navArgument("season") { type = NavType.IntType; defaultValue = 0 },
        navArgument("episode") { type = NavType.IntType; defaultValue = 0 }
    )
) { backStackEntry ->
    val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
    val season = backStackEntry.arguments?.getInt("season") ?: 0
    val episode = backStackEntry.arguments?.getInt("episode") ?: 0
    
    PlayerScreen(subjectId, season, episode)
}
```

## 5️⃣ Configure APIs

**Edit:** `app/src/main/java/com/example/data/remote/ApiClient.kt`
```kotlin
private const val PROD_API_BASE = "https://your-api.com/api"  // ← Change
```

**Edit:** `app/src/main/java/com/example/data/remote/providers/MovieBoxProvider.kt`
```kotlin
private const val API_BASE = "https://moviebox-api.com"  // ← Change
```

## 6️⃣ Enable Debug Logging

**Edit:** `app/src/main/java/com/example/data/remote/ApiClient.kt`
```kotlin
private const val LOG_LEVEL = "debug"  // Options: debug, info, warn, error
```

**View logs:**
```bash
adb logcat | grep StreamWave
```

## 7️⃣ Clear Cache Programmatically

```kotlin
viewModelScope.launch {
    repository.clearCache()  // Clear everything
    repository.clearExpiredCache()  // Only expired entries
}
```

## 8️⃣ Add Custom Provider

```kotlin
class MyProvider : StreamProvider {
    override suspend fun getStreams(...) = ProviderStreamResponse(
        provider = "MyProvider",
        success = true,
        streams = listOf(Stream(...))
    )
    
    override suspend fun getCaptions(...) = emptyList()
}

// Register in ProviderRegistry.providers
```

## 9️⃣ Handle Errors

```kotlin
if (!result.success) {
    // Show error UI
    Snackbar.make(view, "No streams available", Snackbar.LENGTH_LONG)
        .setAction("Retry") { viewModel.loadStreams(...) }
        .show()
}
```

## 🔟 Test Provider Connectivity

```kotlin
// In any coroutine scope
runBlocking {
    val result = MovieBoxProvider.getStreams("test-id", season=1, episode=1)
    Log.d("Test", "Success: ${result.success}")
    Log.d("Test", "Streams: ${result.streams.size}")
}
```

## 📊 Data Models Quick Ref

```kotlin
// Stream object
Stream(
    id = "stream-id",
    provider = "MovieBox",
    url = "https://...",
    type = "hls",        // or "mp4"
    quality = "1080p",
    numericQuality = 1080
)

// Result object
ResolvedStreamPayload(
    success = true,
    streams = listOf(...),
    selectedStream = Stream(...),
    attempts = listOf(...),
    fromCache = false
)
```

## 🎮 Player Controls

- **Play/Pause:** Icon or Space key
- **Seek:** Tap progress bar or Arrow keys
- **Quality:** Settings icon dropdown
- **Speed:** Speed icon dropdown (0.25x to 2x)
- **Captions:** CC icon for subtitles
- **Double-Click:** Left -10s, Right +10s
- **Fullscreen:** Corner icon

## 🔐 Security Checklist

- [ ] Update API URLs to production
- [ ] Move Bearer tokens to Android Keystore
- [ ] Use HTTPS for all URLs
- [ ] Enable certificate pinning (optional)
- [ ] Test on real devices
- [ ] Monitor error logs

## 📱 Device Testing

```bash
# Build and install
./gradlew installDebug

# View logs
adb logcat | grep StreamWave

# Check cache database
adb shell run-as com.example sqlite3 /data/data/com.example/databases/streamwave.db

# Clear app data
adb shell pm clear com.example
```

## ⚡ Performance Tips

1. **Increase cache TTL** for frequently watched:
   ```kotlin
   const val CACHE_TTL_MS = 7200000L  // 2 hours
   ```

2. **Reduce validation timeout** for fast networks:
   ```kotlin
   connectTimeout(5, TimeUnit.SECONDS)
   ```

3. **Preload next episode** while watching:
   ```kotlin
   viewModel.preloadStreams(subjectId, season, episode + 1)
   ```

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| No streams found | Check API URLs, verify provider APIs working |
| Streams not validating | Check URL format, content-type headers |
| Cache not working | Run `repository.clearCache()`, check permissions |
| ExoPlayer errors | Verify URL accessibility, check firewall |
| Crashes on play | Check minimum Android API 24, verify permissions |

## 📋 File Quick Links

| Need | File |
|------|------|
| Setup | `STREAMWAVE_INTEGRATION_GUIDE.md` |
| Architecture | `STREAMWAVE_ARCHITECTURE.md` |
| Examples | `STREAMWAVE_USAGE_EXAMPLES.md` |
| All Files | `FILE_MANIFEST.md` |

## ✅ Launch Checklist

- [ ] Update API URLs
- [ ] Create ViewModel
- [ ] Create PlayerScreen
- [ ] Add to navigation
- [ ] Test with real APIs
- [ ] Add error handling
- [ ] Test on device
- [ ] Check logs for errors
- [ ] Configure production URLs
- [ ] Monitor performance

## 🎯 Key Constants

```kotlin
// Timeouts
API_TIMEOUT = 15000ms          // 15 seconds
VALIDATION_TIMEOUT = 8000ms    // 8 seconds
CONTROL_AUTO_HIDE = 3000ms     // 3 seconds

// Cache
CACHE_TTL_MS = 3600000ms       // 1 hour

// Playback
SKIP_DISTANCE = 10000ms        // 10 seconds
PLAYBACK_SPEEDS = [0.25, 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2]

// Stream Types
HLS = "hls"                    // Adaptive streaming
MP4 = "mp4"                    // Progressive download
```

## 📞 API Reference Snippets

```kotlin
// Get streams
repository.getStreams(
    subjectId: String,
    detailPath: String = "",
    season: Int = 0,
    episode: Int = 0
): ResolvedStreamPayload

// Get captions
repository.getCaptions(
    id: String,
    subjectId: String,
    detailPath: String = ""
): List<Caption>

// Cache operations
repository.clearCache()
repository.clearExpiredCache()
```

---

**Copy these snippets into your code for instant setup!**

For complete examples, see `STREAMWAVE_USAGE_EXAMPLES.md`
