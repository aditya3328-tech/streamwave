# StreamWave Architecture - Android Implementation

Complete web-like streaming architecture ported to Android, matching the reference web implementation.

## Architecture Overview

The architecture follows a layered design inspired by clean architecture principles:

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                       │
│            StreamWaveVideoPlayer Component                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  ViewModel/Repository                        │
│              StreamRepository (High-level API)               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Domain Layer                              │
│  StreamResolver (Orchestration & Fallback Logic)            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Data Layer                                │
│  ├─ Remote: Providers (MovieBox, ProviderA)               │
│  ├─ Local: Cache (Room Database)                           │
│  ├─ Utilities: Validators, API Client                      │
└─────────────────────────────────────────────────────────────┘
```

## Components

### 1. API Client (`ApiClient.kt`)
**Equivalent to web's axios.js**

- Centralized HTTP client using Retrofit + OkHttp
- Request/response logging with conditional levels
- 15-second timeout configuration
- Standard headers (lang, versioncode, clienttype)
- OkHttp interceptors for transparent logging
- Error handling for all status codes

```kotlin
// Usage
val retrofit = ApiClient.retrofitClient
val service = retrofit.create(ApiService::class.java)
ApiClient.log("info", "Starting operation", "details")
```

### 2. API Endpoints (`ApiEndpoints.kt`)
**Equivalent to web's endpoints.js**

Centralized endpoint path definitions:
```kotlin
ApiEndpoints.PLAY          // /subject/play
ApiEndpoints.CAPTION       // /subject/caption
ApiEndpoints.SEARCH        // /subject/search
ApiEndpoints.RECOMMENDATIONS  // /subject/detail-rec
```

### 3. Stream Models (`StreamModels.kt`)

#### Stream
Normalized stream from any provider:
```kotlin
data class Stream(
    val id: String,
    val provider: String,      // "MovieBox", "ProviderA"
    val url: String,
    val type: String,          // "hls" or "mp4"
    val quality: String,       // "1080p", "720p"
    val numericQuality: Int,   // 1080, 720
    val contentType: String?
)
```

#### ProviderStreamResponse
Structured response from any provider:
```kotlin
data class ProviderStreamResponse(
    val provider: String,
    val success: Boolean,
    val streams: List<Stream>,
    val message: String,
    val error: String?
)
```

#### ResolvedStreamPayload
Final result from stream resolver:
```kotlin
data class ResolvedStreamPayload(
    val success: Boolean,
    val streams: List<Stream>,           // All validated streams
    val selectedStream: Stream?,         // Best quality selected
    val attempts: List<ProviderStreamResponse>,  // All provider attempts
    val fromCache: Boolean
)
```

### 4. Stream Validators (`StreamValidator.kt`)
**Equivalent to web's validators.js**

Stream type detection, normalization, and validation:

#### detectStreamType(url: String)
Detects stream type from URL:
```kotlin
".m3u8" → "hls"
".mp4"  → "mp4"
```

#### parseQuality(label: String)
Extracts numeric quality from strings:
```kotlin
"1080p" → 1080
"720p"  → 720
"Unknown" → 0
```

#### normalizeStream(item, provider)
Converts various provider response formats to standard Stream:
- Handles different JSON structure variations
- Extracts URL, quality, metadata
- Determines stream type
- Calculates numeric quality
- Returns null if invalid

#### validateStreamUrl(url: String) - Suspend Function
Validates stream URL with HEAD request:
- 8-second timeout
- Checks content-type against whitelist:
  - HLS: application/vnd.apple.mpegurl, video/MP2T
  - MP4: video/mp4, application/octet-stream
- Returns: {valid, contentType, type}

```kotlin
val result = StreamValidator.validateStreamUrl(url)
if (result.valid) { /* use stream */ }
```

### 5. Provider Adapters

#### StreamProvider Interface
Standardized contract for all providers:
```kotlin
interface StreamProvider {
    suspend fun getStreams(
        subjectId: String,
        detailPath: String = "",
        season: Int = 0,
        episode: Int = 0
    ): ProviderStreamResponse
    
    suspend fun getCaptions(
        id: String,
        subjectId: String,
        detailPath: String
    ): List<Caption>
}
```

#### MovieBoxProvider (`MovieBoxProvider.kt`)
**Equivalent to web's moviebox.js**

Adapts MovieBox API responses:
- Multiple response format support:
  - `payload.streams` array
  - `payload.list` array
  - `payload.videoUrl` direct URL
- Normalizes each stream
- Deduplicates by URL
- Comprehensive error handling

```kotlin
// Usage
val response = MovieBoxProvider.getStreams(
    subjectId = "123",
    season = 1,
    episode = 5
)
```

#### ProviderA (`ProviderA.kt`)
Alternative provider with simpler interface:
- Configuration check (PROVIDER_A_API_BASE environment variable)
- Simpler response structure
- Fallback when MovieBox unavailable

### 6. Stream Caching (`StreamCache.kt`)
**Equivalent to web's cache.js + Room Database**

#### StreamCacheEntity (Room)
```kotlin
@Entity(tableName = "stream_cache")
data class StreamCacheEntity(
    val cacheKey: String,
    val streamsJson: String,
    val selectedStreamJson: String?,
    val timestamp: Long,
    val requestJson: String,
    val providerName: String
)
```

TTL: 1 hour (3600000ms)

#### StreamCacheManager
High-level cache operations:
```kotlin
// Get cached streams
val cached = cacheManager.getCachedStreams(cacheKey)

// Cache streams
cacheManager.cacheStreams(
    cacheKey = "stream_123_s1_e5",
    streams = validStreams,
    selectedStream = bestStream,
    providerName = "MovieBox",
    requestJson = requestData
)

// Clear cache
cacheManager.clearCache()
cacheManager.clearExpiredCache()
```

#### StreamWaveDatabase (Room)
```kotlin
val db = StreamWaveDatabase.getInstance(context)
val cacheManager = StreamCacheManager(db.streamCacheDao())
```

### 7. Stream Resolver (`StreamResolver.kt`)
**Equivalent to web's streamResolver.js**

Core orchestration for multi-provider stream resolution:

**Algorithm:**
1. Generate cache key from request
2. Check cache; return if hit
3. Iterate through providers: [MovieBox, ProviderA]
4. For each provider:
   - Call getStreams()
   - Normalize response
   - Validate each stream (parallel HEAD requests)
   - If valid streams found: select best, break
5. Cache result with TTL
6. Return payload with all details

**selectBestStream Algorithm:**
Sort streams by:
1. Numeric quality (DESC) - highest first
2. Stream type - HLS > MP4
3. Order - first in list

```kotlin
val resolver = StreamResolver(cacheManager)
val result = resolver.resolveStreams(
    StreamRequest(
        subjectId = "123",
        detailPath = "/anime/...",
        se = 1,
        ep = 5
    )
)

if (result.success) {
    val stream = result.selectedStream  // Best quality
    val all = result.streams            // All valid options
}
```

### 8. Stream Repository (`StreamRepository.kt`)
**High-level API** - Single entry point for all streaming operations:

```kotlin
val repository = StreamRepository(context)

// Main operation: resolve streams with fallback
val payload = repository.getStreams(
    subjectId = "123",
    season = 1,
    episode = 5
)

// Get captions
val captions = repository.getCaptions(
    id = "123",
    subjectId = "456"
)

// Cache management
repository.clearCache()
repository.clearExpiredCache()
```

### 9. Video Player Component (`StreamWaveVideoPlayer.kt`)
**Equivalent to web's Player.jsx**

Full-featured Compose video player:

#### Features
- **Stream Formats**: HLS and MP4 support via ExoPlayer
- **Quality Selection**: Switch between available streams
- **Subtitles**: Caption/subtitle support with language selection
- **Playback Speed**: 0.25x to 2x with 8 preset options
- **Progress Tracking**: Seek bar with time display
- **Double-Click Skip**: Left half -10s, right half +10s
- **Controls Auto-Hide**: 3-second timeout
- **Keyboard Shortcuts**: Space (play), F (fullscreen), M (mute), Arrows (seek/volume)
- **Picture-in-Picture**: Desktop fullscreen support
- **Episode Navigation**: Previous/next episode buttons

#### Usage
```kotlin
@Composable
fun VideoScreen() {
    val repository = remember { StreamRepository(context) }
    
    StreamWaveVideoPlayer(
        streams = streams,
        captions = captions,
        selectedStream = selectedStream,
        onStreamSelected = { stream ->
            // Quality changed
        },
        onCaptionSelected = { caption ->
            // Caption changed
        },
        onNextEpisode = {
            // Load next episode
        },
        onPreviousEpisode = {
            // Load previous episode
        }
    )
}
```

#### ExoPlayer Integration
- Uses Media3 (androidx.media3)
- Transparent HLS/MP4 handling
- Adaptive bitrate streaming support
- Hardware acceleration available

## Data Flow

### Stream Resolution Flow
```
StreamRepository.getStreams()
    ↓
StreamResolver.resolveStreams()
    ├─ Check cache
    ├─ Try MovieBoxProvider.getStreams()
    │   ├─ Parse response
    │   ├─ Normalize streams
    │   └─ Validate URLs (parallel)
    ├─ Try ProviderA.getStreams() (if needed)
    ├─ Select best stream
    ├─ Cache result (TTL: 1 hour)
    └─ Return ResolvedStreamPayload
```

### UI Binding Flow
```
ViewModel.getStreams()
    ↓
StreamRepository.getStreams()
    ↓
ExoPlayer receives URL
    ↓
StreamWaveVideoPlayer renders
    ├─ Progress tracking
    ├─ Quality selector
    ├─ Caption selector
    ├─ Speed control
    └─ Episode navigation
```

## Configuration

### Environment Variables
Set in your `.env` or system environment:

```
VITE_STREAM_BACKEND_URL=https://h5-api.aoneroom.com/wefeed-h5api-bff
VITE_API_TIMEOUT=15000
VITE_LOG_LEVEL=warn
PROVIDER_A_API_BASE=https://provider-a.example.com
```

### API Base URLs
Edit in `ApiClient.kt`:
```kotlin
private const val PROD_API_BASE = "https://h5-api.aoneroom.com/wefeed-h5api-bff"
```

### Provider Configuration
MovieBox uses hard-coded API:
```kotlin
private const val API_BASE = "https://moviebox-api.example.com"
```

ProviderA uses environment variable:
```kotlin
private val API_BASE: String? = System.getenv("PROVIDER_A_API_BASE")
```

## Error Handling

### Provider Errors
- Returns `ProviderStreamResponse` with success=false
- Includes error message and exception details
- Falls back to next provider automatically

### Validation Errors
- URL validation failures return `{valid: false}`
- Invalid content-types rejected
- Timeout errors (8 seconds) handled gracefully

### Cache Errors
- Expired entries automatically cleaned
- Corrupted cache entries skipped
- Falls back to fresh provider request

## Performance Optimizations

1. **Parallel Validation**: Stream URLs validated in parallel using Kotlin coroutines
2. **TTL Caching**: 1-hour cache prevents redundant requests
3. **Early Termination**: Stops at first successful provider
4. **Connection Pooling**: OkHttp client manages connections efficiently
5. **Lazy Initialization**: Resources created on-demand

## Security Considerations

1. **Bearer Token**: Move from hard-coded to environment configuration
2. **HTTPS Only**: Use HTTPS URLs for all API calls
3. **Certificate Pinning**: Optional via OkHttp configuration
4. **Content-Type Validation**: Prevents stream injection attacks
5. **User-Agent Rotation**: Optional per provider

## Testing

Unit test examples:

```kotlin
@Test
fun testStreamValidation() {
    val type = StreamValidator.detectStreamType("test.m3u8")
    assertEquals("hls", type)
}

@Test
fun testQualityParsing() {
    val quality = StreamValidator.parseQuality("1080p")
    assertEquals(1080, quality)
}

@Test
fun testCacheKey() {
    val key = makeCacheKey(StreamRequest("123", se=1, ep=5))
    assertEquals("stream_123_s1_e5", key)
}

@Test
fun testBestStreamSelection() {
    val streams = listOf(
        Stream(url="a.m3u8", quality="720p", numericQuality=720, type="hls"),
        Stream(url="b.mp4", quality="1080p", numericQuality=1080, type="mp4")
    )
    val best = selectBestStream(streams)
    assertEquals("b.mp4", best.url)  // 1080p takes priority
}
```

## Dependency Injection (Optional)

Integrate with Hilt for dependency injection:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object StreamingModule {
    @Provides
    @Singleton
    fun provideStreamRepository(context: Context): StreamRepository {
        return StreamRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideStreamResolver(
        cacheManager: StreamCacheManager
    ): StreamResolver {
        return StreamResolver(cacheManager)
    }
}

// Usage in ViewModel
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val streamRepository: StreamRepository
) : ViewModel() {
    fun getStreams(subjectId: String) = viewModelScope.launch {
        val result = streamRepository.getStreams(subjectId)
    }
}
```

## File Structure

```
app/src/main/java/com/example/
├── data/
│   ├── local/
│   │   ├── StreamCache.kt (Entity, DAO, Manager)
│   │   └── StreamWaveDatabase.kt (Room Database)
│   ├── model/
│   │   └── StreamModels.kt (Data classes)
│   ├── remote/
│   │   ├── ApiClient.kt (Retrofit + OkHttp)
│   │   ├── ApiEndpoints.kt (Endpoint constants)
│   │   ├── StreamValidator.kt (Validation utilities)
│   │   ├── StreamResolver.kt (Core orchestration)
│   │   └── providers/
│   │       ├── StreamProvider.kt (Interface + Registry)
│   │       ├── MovieBoxProvider.kt
│   │       └── ProviderA.kt
│   └── repository/
│       └── StreamRepository.kt (High-level API)
├── ui/
│   ├── components/
│   │   └── StreamWaveVideoPlayer.kt (Compose video player)
│   ├── screens/
│   ├── viewmodel/
│   └── theme/
└── MainActivity.kt
```

## Migration from Web

| Web Component | Android Equivalent |
|---|---|
| axios.js | ApiClient.kt + Retrofit |
| moviebox.js | MovieBoxProvider.kt |
| validators.js | StreamValidator.kt |
| streamResolver.js | StreamResolver.kt |
| cache.js | StreamCache.kt + Room |
| endpoints.js | ApiEndpoints.kt |
| Player.jsx | StreamWaveVideoPlayer.kt |
| Zustand stores | ViewModel + LiveData |

## Next Steps

1. **Configure API endpoints** in `ApiClient.kt` and provider classes
2. **Set environment variables** for production APIs
3. **Implement ViewModel** to manage UI state
4. **Add error UI** for failed stream resolution
5. **Integrate with existing screens** using StreamWaveVideoPlayer
6. **Add unit/integration tests** for providers and resolver
7. **Implement analytics** for provider selection and playback
8. **Add DRM/encryption** support if needed
