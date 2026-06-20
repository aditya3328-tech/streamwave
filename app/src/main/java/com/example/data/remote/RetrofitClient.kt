package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val API_BASE_URL = "https://h5-api.aoneroom.com/wefeed-h5api-bff"
    private const val TIMEOUT_SECONDS = 15L
    private const val MAX_RETRIES = 3

    // Thread-safe cached Moshi instance
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Centralized OkHttpClient with Interceptors & Custom Retry/Timeout Configs
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(HeaderInterceptor())
            .addInterceptor { chain ->
                val request = chain.request()
                val urlString = request.url.toString()
                
                // Read Request Body safely
                val requestBody = request.body
                var requestBodyString = ""
                if (requestBody != null) {
                    try {
                        val buffer = okio.Buffer()
                        requestBody.writeTo(buffer)
                        requestBodyString = buffer.readUtf8()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                
                // Log Search URL (TASK 2)
                if (urlString.contains("subject/search") || urlString.contains("subject/search-suggest")) {
                    android.util.Log.d("SEARCH_URL", urlString)
                }
                
                android.util.Log.d("REQUEST_URL", urlString)
                android.util.Log.d("REQUEST_HEADERS", request.headers.toString())
                if (requestBodyString.isNotEmpty()) {
                    android.util.Log.d("REQUEST_BODY", requestBodyString)
                }
                
                val response = chain.proceed(request)
                
                // Read Response Body safely without consuming the stream
                val responseBody = response.body
                var responseBodyString = ""
                if (responseBody != null) {
                    try {
                        val source = responseBody.source()
                        source.request(Long.MAX_VALUE)
                        val buffer = source.buffer
                        responseBodyString = buffer.clone().readString(java.nio.charset.Charset.forName("UTF-8"))
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                
                if (responseBodyString.isNotEmpty()) {
                    android.util.Log.d("RESPONSE_BODY", responseBodyString)
                    
                    if (urlString.contains("subject/play")) {
                        android.util.Log.d("PLAY_RESPONSE_BODY", responseBodyString)
                    } else if (urlString.contains("subject/search") && !urlString.contains("suggest")) {
                        android.util.Log.d("SEARCH_RESPONSE", responseBodyString)
                    } else if (urlString.contains("subject/filter")) {
                        android.util.Log.d("FILTER_RESPONSE", responseBodyString)
                    } else if (urlString.contains("detail")) {
                        android.util.Log.d("DETAIL_RESPONSE", responseBodyString)
                    }
                }
                
                response
            }
            .addInterceptor(RetryInterceptor(MAX_RETRIES))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    // Centralized Retrofit Instance
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("$API_BASE_URL/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // Lazy API Service implementation
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    // 1. Dynamic/Static Headers Interceptor
    private class HeaderInterceptor : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val host = originalRequest.url.host

            // CRITICAL FIX: movie-box.co/subject/play does NOT accept Authorization.
            // The web client never sends Authorization to this endpoint.
            // Sending it causes the API to reject the request or return an error body
            // which Moshi parses as data=null → streams=null → empty list.
            val isMovieBoxPlayEndpoint = host.contains("movie-box.co")

            val requestBuilder = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("lang", "en")
                .header("versioncode", "11")
                .header("clienttype", "0")

            // Only add Authorization for the BFF proxy (aoneroom.com), not for movie-box.co
            if (!isMovieBoxPlayEndpoint) {
                requestBuilder.header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjg5NDQ2ODQzMjM3MzI2MDg1MTIsImF0cCI6MywiZXh0IjoiMTc4MDI4ODA4MiIsImV4cCI6MTc4ODA2NDA4MiwiaWF0IjoxNzgwMjg3NzgyfQ.30FAw16jFMuEHeUfjmSN1atbeYSZUn4QHFIxp3repbI")
                requestBuilder.header("Content-Type", "application/json")
            }

            return chain.proceed(requestBuilder.build())
        }
    }

    // 2. Custom Retry Logic Interceptor — only retries on network-level IOException,
    // NOT on HTTP 4xx/5xx responses (those are server errors, not transient network issues).
    private class RetryInterceptor(private val maxRetries: Int) : Interceptor {
        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var lastException: IOException? = null
            var tryCount = 0

            while (tryCount < maxRetries) {
                try {
                    val response = chain.proceed(request)
                    // Return immediately on any HTTP response (success OR error).
                    // Do NOT retry HTTP errors — they are deterministic server responses.
                    return response
                } catch (e: IOException) {
                    // Only retry on true network failures (no connection, timeout, etc.)
                    lastException = e
                    android.util.Log.w("RETRY", "Network error on attempt ${tryCount + 1}/$maxRetries: ${e.message}")
                }
                tryCount++
            }

            throw lastException ?: IOException("Network request failed after $maxRetries retries")
        }
    }
}
