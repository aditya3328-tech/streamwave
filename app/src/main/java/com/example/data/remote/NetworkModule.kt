package com.example.data.remote

import android.content.Context
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Direct MovieBox network stack — no localhost, no custom backend.
 *
 * Browser-equivalent session flow:
 *   1. GET https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/caption
 *        → Set-Cookie: token=... , x-user: {"token":"..."}   (session bootstrap)
 *   2. GET https://netfilm.world/wefeed-h5api-bff/subject/play
 *        → sent WITH the token cookie captured in step 1 → success=true, streams>0
 *        (netfilm.world is the current website host; Referer/Origin also use it)
 *
 * One shared, persistent [CookieJar] + [MovieBoxSession] carry the token across BOTH hosts.
 */
object NetworkModule {

    // Current browser traffic posts /subject/play to the site host netfilm.world
    // (which proxies /wefeed-h5api-bff/* to the API). Caption still bootstraps the session
    // on the API origin h5-api.aoneroom.com. Both share the CookieJar + MovieBoxSession.
    // (netfilm.world replaced the old movie-box.co site domain.)
    const val PLAY_BASE_URL = "https://netfilm.world/wefeed-h5api-bff/"
    const val CAPTION_BASE_URL = "https://h5-api.aoneroom.com/wefeed-h5api-bff/"
    private const val TRACE = "MOVIEBOX_TRACE"

    fun init(context: Context) = MovieBoxSession.init(context)

    /**
     * Persistent, cross-host cookie jar. Captures every Set-Cookie from netfilm.world and
     * h5-api.aoneroom.com, promotes token / mb_token into [MovieBoxSession] (SharedPreferences),
     * and replays ALL stored cookies on every request to either host.
     */
    val cookieJar = object : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

        private fun isMovieBoxHost(host: String): Boolean =
            host.endsWith("netfilm.world") || host.endsWith("aoneroom.com")

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            val bucket = store.getOrPut(url.host) { mutableListOf() }
            cookies.forEach { c ->
                bucket.removeAll { it.name == c.name }
                bucket.add(c)
                Log.d(TRACE, "COOKIE <= saved host=${url.host} ${c.name}=${c.value}")
                when (c.name) {
                    "token" -> MovieBoxSession.setToken(c.value, "Set-Cookie@${url.host}")
                    "mb_token" -> MovieBoxSession.setMbToken(c.value, "Set-Cookie@${url.host}")
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            // Send cookies stored for ANY MovieBox host (caption's token → play request).
            val all = if (isMovieBoxHost(url.host)) {
                store.values.flatten()
            } else {
                store[url.host] ?: emptyList()
            }
            val valid = all.filter { it.expiresAt >= now }.distinctBy { it.name }
            if (valid.isNotEmpty()) {
                Log.d(TRACE, "COOKIE => send host=${url.host} [${valid.joinToString { "${it.name}=${it.value}" }}]")
            }
            return valid
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(MovieBoxInterceptor())
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    /** /subject/play → netfilm.world */
    val playApi: ApiService by lazy { retrofit(PLAY_BASE_URL).create(ApiService::class.java) }

    /** /subject/caption → h5-api.aoneroom.com (session bootstrap) */
    val captionApi: ApiService by lazy { retrofit(CAPTION_BASE_URL).create(ApiService::class.java) }

    // Back-compat alias (older callers used NetworkModule.movieBoxApi for play).
    val movieBoxApi: ApiService get() = playApi
}
