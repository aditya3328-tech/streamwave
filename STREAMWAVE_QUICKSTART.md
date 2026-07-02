# StreamWave Quick Start - Testing & Deployment

## 🚀 5-Minute Setup

### Step 1: Configure Provider Keys (1 min)

Edit `.env` file:
```bash
# Replace with your actual provider API keys
MOVIEBOX_API_BASE=https://api.moviebox.dev
MOVIEBOX_API_KEY=your_actual_key_here
PROVIDER_A_API_BASE=https://api.providera.dev
PROVIDER_A_API_KEY=your_actual_key_here
```

### Step 2: Clean Build (2 min)

```bash
cd c:\Users\adity\Downloads\cineverse (1)

# Clean previous build
./gradlew.bat clean

# Build fresh
./gradlew.bat build
```

### Step 3: Install (1 min)

```bash
# Debug install (emulator or device)
./gradlew.bat installDebug

# Or release build
./gradlew.bat installRelease
```

### Step 4: Test Streams (1 min)

Open logcat and filter by `StreamWave`:
```
Log.d("StreamWave", "✅ Resolved stream: 1080p HLS")
```

**Expected in logcat:**
```
PlayerScreen: Calling backend to resolve stream...
StreamResolver: Attempting MovieBox provider...
StreamResolver: Validating 3 streams in parallel...
StreamWave: ✅ Resolved stream: 1080p HLS from MovieBox
PlayerScreen: Initializing ExoPlayer with URL...
```

## ✅ Verification Checklist

### Network & Provider Setup
- [ ] `.env` file has real provider API keys (not placeholder)
- [ ] Network connectivity confirmed (ping your provider API)
- [ ] API keys are valid and active
- [ ] CORS headers allow from app domain

### Build Verification
- [ ] `./gradlew clean build` completes without errors
- [ ] BuildConfig.java contains real URLs (not placeholder)
- [ ] All dependencies resolved
- [ ] APK generated successfully

### Runtime Verification
- [ ] App starts without crashes
- [ ] "Resolve stream..." message appears in logcat
- [ ] Streams successfully loaded (check logcat tags):
  - `StreamResolver`
  - `StreamWave`
  - `PlayerScreen`
- [ ] Player displays with valid URL

### Playback Verification
- [ ] Video starts playing
- [ ] Quality selector works
- [ ] Caption selector works (if available)
- [ ] Speed control works
- [ ] Episode navigation works
- [ ] No buffering errors in logcat

## 🧪 Test Scenarios

### Scenario 1: Happy Path (All Working)
```
Expected flow:
User clicks episode
  ↓
Logcat shows: "Attempting MovieBox provider..."
Logcat shows: "Validating 3 streams in parallel..."
Logcat shows: "✅ Resolved stream: 1080p HLS"
Player starts with video
  ↓
Result: ✅ SUCCESS
```

### Scenario 2: Provider Fallback
```
If MovieBox fails:
Logcat shows: "❌ MovieBox failed: [error message]"
Logcat shows: "Attempting ProviderA provider..."
Logcat shows: "✅ Resolved stream from ProviderA"
Player starts with video
  ↓
Result: ✅ FALLBACK SUCCESS
```

### Scenario 3: Both Providers Fail
```
If both providers fail:
Logcat shows: "❌ MovieBox failed: [error]"
Logcat shows: "❌ ProviderA failed: [error]"
Logcat shows: "Error screen: No streams available"
Retry button appears
User can retry
  ↓
Result: ✅ ERROR HANDLED
```

### Scenario 4: Cache Hit
```
First load (10 seconds):
Logcat shows: "fromCache: false"
  ↓
Second identical load (immediate):
Logcat shows: "fromCache: true"
Result: ✅ CACHE WORKING
```

## 🔍 Debugging Tips

### Check Stream Resolution
```kotlin
// In PlayerViewModel or integration
val result = repository.getStreams(subjectId, season, episode)
Log.d("DEBUG", "Success: ${result.success}")
Log.d("DEBUG", "Streams: ${result.streams.size}")
Log.d("DEBUG", "From cache: ${result.fromCache}")
Log.d("DEBUG", "Provider: ${result.selectedStream?.provider}")
Log.d("DEBUG", "Quality: ${result.selectedStream?.quality}")
```

### Check Network Calls
```bash
# In Android Studio Logcat, filter by:
# OkHttpClient - shows all HTTP requests/responses
# StreamResolver - shows provider attempts
# StreamValidator - shows URL validation
```

### Check ExoPlayer Initialization
```bash
# In Android Studio Logcat, filter by:
# EXOPLAYER - shows player errors
# PlayerScreen - shows player events
```

### Common Issues & Fixes

| Issue | Cause | Fix |
|-------|-------|-----|
| "Unable to resolve host" | Wrong API base URL in .env | Update .env with real URL |
| 401 Unauthorized | Invalid API key | Verify API key in .env |
| Empty stream list | Provider API format changed | Check MovieBoxProvider.kt normalization |
| Player doesn't start | Invalid URL returned | Enable logcat filtering on EXOPLAYER |
| Streams not cached | TTL expired | Clear cache or wait 1 hour |
| Playback error | Network issue during stream | Check stream URL accessibility |

## 📊 Performance Monitoring

### Key Metrics to Monitor
1. **Stream Resolution Time**
   - Target: < 5 seconds (first load)
   - Target: < 10ms (cache hit)
   - Monitor in logcat with timestamps

2. **Provider Fallback Time**
   - If MovieBox fails: < 1 second to switch to ProviderA
   - Check logs for provider attempt times

3. **Quality Selection**
   - Verify highest quality is selected
   - Check logcat: `selectBestStream: Selected 1080p HLS`

4. **Caching Efficiency**
   - Monitor cache hits vs API calls
   - Should be > 90% hits after first load

### Enable Performance Logging
```kotlin
// In ApiClient.kt, set debug level
LOG_LEVEL = HttpLoggingInterceptor.Level.BODY  // Maximum logging
```

## 🚦 Deployment Checklist

### Pre-Deployment
- [ ] All provider API keys configured
- [ ] Clean build completed successfully
- [ ] Testing scenarios verified
- [ ] Performance acceptable
- [ ] No critical errors in logcat
- [ ] Documentation updated

### Deployment
- [ ] Sign APK with release key
- [ ] Test on multiple devices
- [ ] Monitor crash reports
- [ ] Monitor stream resolution success rate

### Post-Deployment
- [ ] Monitor error rates in logcat
- [ ] Verify cache efficiency
- [ ] Check provider API stability
- [ ] Monitor user feedback

## 🎬 Example Usage in Code

### Use Case 1: Load Episode
```kotlin
// In PlayerViewModel or Composable
viewModel.loadStreams(
    subjectId = "movie-123",
    season = 1,
    episode = 5
)
```

### Use Case 2: Switch Quality
```kotlin
// In UI after streams loaded
viewModel.selectStream(streams[0])  // Switch to first stream
```

### Use Case 3: Change Episode
```kotlin
// Navigation buttons
viewModel.nextEpisode()       // Load next episode
viewModel.previousEpisode()   // Load previous episode
```

### Use Case 4: Manual Cache Clear
```kotlin
// For force refresh
viewModel.clearCache()
viewModel.retry()  // Reload after clearing
```

## 📞 Support

### Logs to Check
- **PlayerScreen** - Player lifecycle
- **StreamResolver** - Provider orchestration
- **StreamWave** - Stream resolution
- **EXOPLAYER** - Video playback
- **OkHttpClient** - Network calls

### Common Log Searches
```
# Stream resolution success
Log: ✅ Resolved stream

# Provider fallback
Log: ❌ MovieBox failed
Log: Attempting ProviderA

# Cache hits
Log: fromCache: true

# Playback errors
Log: === PLAYBACK ERROR ===
```

## ✨ Success Indicators

When everything is working:

✅ App starts without crashes
✅ "Resolve stream..." shows in logcat
✅ Provider logs show "✅ Resolved stream"
✅ Player displays with video URL
✅ Video plays without buffering errors
✅ Quality switching works
✅ Captions load properly
✅ Cache hits after first load

If all above are true → **PLAYBACK IS WORKING** 🎬
