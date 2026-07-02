package com.example.sports.api

import com.example.sports.model.Match
import com.example.sports.model.MatchResponse
import com.example.sports.model.News
import com.example.sports.model.SportsResponse
import com.example.sports.model.SportsSection
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit service for the standalone Sports BFF.
 *
 * Base URL: https://h5-sport-api.aoneroom.com/wefeed-h5api-bff/
 *
 * NOTE: the exact endpoint paths below are the best-known SportsNow routes. Only
 * `/news/trending-tag` is specified explicitly; the others mirror the same naming. The
 * [com.example.sports.repository.SportsRepository] degrades gracefully (sample data) if any
 * call fails, so the UI is always functional while real paths are confirmed.
 */
interface SportsApiService {

    /** Home screen sections: Trending leagues, World Cup, Premier League, Cricket, Basketball… */
    @POST("match/home")
    suspend fun getHomeSections(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): SportsResponse<List<SportsSection>>

    /** Flat, filterable match list for the "Matches" top tab. */
    @POST("match/list")
    suspend fun getMatches(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): SportsResponse<List<Match>>

    /** Single fixture detail (teams, odds, highlights, play sources, related news). */
    @GET("sport/detail-v1")
    suspend fun getMatchDetails(
        @Query("matchId") matchId: String,
    ): MatchResponse

    /** Trending news grouped by tag — the explicitly specified endpoint. */
    @POST("news/trending-tag")
    suspend fun getTrendingNews(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): SportsResponse<List<News>>

    /** Full article body + related news. */
    @POST("news/detail")
    suspend fun getNewsDetail(
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): SportsResponse<News>
}
