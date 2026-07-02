package com.example.sports.api

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Self-contained network stack for the Sports module — its OWN OkHttpClient, cookie jar,
 * Moshi instance and Retrofit. It shares nothing with [com.example.data.remote.RetrofitClient]
 * or [com.example.data.remote.NetworkModule] (the movie stacks).
 *
 * The cookie jar persists every cookie from the sport host and also promotes the `token`
 * cookie into [SportsSession] (SharedPreferences) so it is replayed automatically.
 */
object SportsNetworkModule {

    private const val BASE_URL = "https://h5-sport-api.aoneroom.com/wefeed-h5api-bff/"

    /** Call once at app start. */
    fun init(context: Context) = SportsSession.init(context)

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val cookieJar = object : CookieJar {
        private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            val bucket = store.getOrPut(url.host) { mutableListOf() }
            cookies.forEach { c ->
                bucket.removeAll { it.name == c.name }
                bucket.add(c)
                if (c.name == "token") SportsSession.setToken(c.value, "CookieJar@${url.host}")
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            return (store[url.host] ?: emptyList()).filter { it.expiresAt >= now }
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(SportsInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** Shared OkHttp client — also used by the Sports ExoPlayer data source for live HLS. */
    val playerHttpClient: OkHttpClient get() = okHttpClient

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: SportsApiService by lazy { retrofit.create(SportsApiService::class.java) }
}
