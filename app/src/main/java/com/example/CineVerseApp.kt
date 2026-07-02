package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okio.Path.Companion.toOkioPath

/**
 * Custom Application that supplies a single, app-wide Coil [ImageLoader].
 *
 * Every `AsyncImage` / `OptimizedPosterImage` call in the app resolves through this
 * singleton, so all of them inherit:
 *  - a bounded in-memory bitmap cache (≈20% of the app heap) → prevents OOM / GC churn,
 *  - a large persistent disk cache (250 MB) with cache headers ignored → aggressive,
 *    offline-friendly reuse of already-downloaded posters,
 *  - hardware bitmaps + crossfade disabled by default → cheaper uploads, less jank.
 */
class CineVerseApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Cap RAM used by decoded bitmaps; keeps the app lightweight.
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(250L * 1024 * 1024) // 250 MB
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // Ignore server cache-control so cached posters are reused aggressively.
            .respectCacheHeaders(false)
            // Global defaults; individual requests can still override.
            .crossfade(false)
            .allowHardware(true)
            .build()
    }
}
