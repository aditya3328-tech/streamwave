package com.example.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MovieTabRetrofitClient {
    const val BASE_URL = "https://h5-api.aoneroom.com/wefeed-h5api-bff/"
    const val TAB_ID = "ONEROOM_MOVIE"
    const val HOST = "movie-box.co"

    private const val ORIGIN = "https://movie-box.co"
    private const val REFERER = "https://movie-box.co/"
    private const val X_CLIENT_INFO = "{\"timezone\":\"Asia/Calcutta\",\"system_language\":\"en\"}"
    private const val X_DEVICE_INFO = "{}"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Origin", ORIGIN)
                    .header("Referer", REFERER)
                    .header("x-client-info", X_CLIENT_INFO)
                    .header("x-device-info", X_DEVICE_INFO)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: MovieTabApi by lazy {
        retrofit.create(MovieTabApi::class.java)
    }
}
