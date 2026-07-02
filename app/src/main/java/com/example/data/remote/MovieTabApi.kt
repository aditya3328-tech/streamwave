package com.example.data.remote

import com.example.data.model.movie.MovieTabOperatingResponse
import com.example.data.model.movie.MovieTrendingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MovieTabApi {
    @GET("tab-operating")
    suspend fun getTabOperating(
        @Query("tabId") tabId: String = MovieTabRetrofitClient.TAB_ID,
        @Query("host") host: String = MovieTabRetrofitClient.HOST,
    ): MovieTabOperatingResponse

    @GET("subject/trending")
    suspend fun getTrendingMovies(
        @Query("tabId") tabId: String = MovieTabRetrofitClient.TAB_ID,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 18,
    ): MovieTrendingResponse
}
