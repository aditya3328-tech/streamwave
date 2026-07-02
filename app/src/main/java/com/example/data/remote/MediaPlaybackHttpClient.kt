package com.example.data.remote

import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Dedicated OkHttp client for ExoPlayer MEDIA requests (the .mp4 / .m3u8 manifest and
 * .ts segment fetches) — NOT the JSON API calls. This is wired into ExoPlayer through
 * androidx.media3.datasource.okhttp.OkHttpDataSource (see PlayerScreen.StreamingPlayer).
 *
 * WHY THIS EXISTS — evidence from the StreamWave website + the live MovieBox CDN
 * ---------------------------------------------------------------------------------
 * The website plays media with a plain browser <video src=...> element
 *   (streamwave/src/components/Player.jsx:437-440).
 * The MovieBox provider attaches its headers ONLY to the resolve call
 *   (streamwave/backend/providers/moviebox.js:15-24) — Referer:
 *   `https://movie-box.co/movies/${detailPath}?id=${subjectId}`.
 *
 * Direct probing of the real signed media URL returned by
 *   https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/play
 * (host bcdnxw.hakunaymatata.com, an Alibaba OSS CDN) proved EXACTLY one requirement:
 *
 *   Request                                              Result
 *   ---------------------------------------------------  ------
 *   No Referer / desktop UA / StreamWave UA              403   <- browser-plain FAILS
 *   Referer: https://movie-box.co/...  (any path/root)   206   <- the ONLY thing that matters
 *   Origin only / UA only / foreign Referer              403
 *   tampered ?sign=                                      403   <- signed query also required
 *
 * Conclusions baked into this client:
 *  - The CDN is a Referer-whitelist CDN: every media hop MUST carry a movie-box.co Referer.
 *    DefaultHttpDataSource.setDefaultRequestProperties applies headers to the first request
 *    only reliably; this client's network interceptor GUARANTEES the Referer survives
 *    redirects, HLS variant playlists, segments and encryption-key fetches.
 *  - UA / Origin / Authorization are NOT required (the `?sign=&t=` query is the auth).
 *    We therefore do NOT inject an Authorization header on media requests (Phase 5).
 *  - A CookieJar is added to mirror the browser: if any title ever returns Set-Cookie /
 *    a non-empty `signCookie` on its manifest, the cookie automatically rides along to the
 *    segment requests — ExoPlayer's default data source has no cookie store, the browser does.
 */
object MediaPlaybackHttpClient {

    private const val TRACE = "CDN_TRACE"

    /** Fallback Referer — proven sufficient on its own (bare host returns 206). */
    const val FALLBACK_REFERER = "https://movie-box.co/"

    /**
     * In-memory cookie jar shared across all media requests of a session.
     * Mirrors the automatic cookie handling a browser gives the website.
     */
    private val cookieJar = object : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            val host = url.host
            val bucket = store.getOrPut(host) { mutableListOf() }
            cookies.forEach { incoming ->
                bucket.removeAll { it.name == incoming.name }
                bucket.add(incoming)
                Log.d(TRACE, "COOKIE <= saved host=$host ${incoming.name}=${incoming.value}")
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            val valid = store[url.host]?.filter { it.expiresAt >= now } ?: emptyList()
            if (valid.isNotEmpty()) {
                Log.d(TRACE, "COOKIE => send host=${url.host} count=${valid.size} [${valid.joinToString { it.name }}]")
            }
            return valid
        }
    }

    /**
     * Network interceptor (runs per actual hop, including redirects) that:
     *  1) guarantees a movie-box.co Referer is present on every media request, and
     *  2) emits a full request/response trace tagged M3U8 / TS / MEDIA.
     */
    private val traceInterceptor = Interceptor { chain ->
        val original = chain.request()

        val request = if (original.header("Referer").isNullOrEmpty()) {
            original.newBuilder().header("Referer", FALLBACK_REFERER).build()
        } else {
            original
        }

        val urlStr = request.url.toString()
        val kind = when {
            urlStr.contains(".m3u8", ignoreCase = true) -> "M3U8"
            urlStr.contains(".ts", ignoreCase = true) ||
                urlStr.contains(".m4s", ignoreCase = true) -> "TS"
            else -> "MEDIA"
        }

        Log.d(TRACE, "──────── $kind REQUEST ────────")
        Log.d(TRACE, "$kind ${request.method} $urlStr")
        request.headers.forEach { (name, value) ->
            Log.d(TRACE, "$kind  > $name: $value")
        }

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TRACE, "$kind !! transport error for $urlStr: ${e.javaClass.simpleName} ${e.message}")
            throw e
        }

        Log.d(TRACE, "──────── $kind RESPONSE ────────")
        Log.d(TRACE, "$kind <= HTTP ${response.code} ${response.message}  ($urlStr)")
        response.headers.forEach { (name, value) ->
            Log.d(TRACE, "$kind  < $name: $value")
        }
        if (!response.isSuccessful) {
            // Surface the body for failures (small html error pages) without consuming the stream.
            val peek = try {
                response.peekBody(2048).string()
            } catch (e: Exception) {
                "<unreadable: ${e.message}>"
            }
            Log.e(TRACE, "$kind <= FAILURE body: $peek")
        }
        response
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addNetworkInterceptor(traceInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
