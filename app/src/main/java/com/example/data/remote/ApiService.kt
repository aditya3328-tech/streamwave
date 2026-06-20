package com.example.data.remote

import com.example.data.model.*
import retrofit2.http.*

interface ApiService {
    // Legacy API Endpoints (keeps current compilation path unbroken)
    @GET("home/feed")
    suspend fun getHomeFeed(): List<MediaSummary>

    @GET("home/trending")
    suspend fun getTrending(): List<MediaSummary>

    @GET("home/popular/movies")
    suspend fun getPopularMovies(): List<MediaSummary>

    @GET("home/popular/tv")
    suspend fun getPopularTVShows(): List<MediaSummary>

    @GET("home/rankings")
    suspend fun getRankings(): List<MediaSummary>



    @GET("details/{id}")
    suspend fun getDetails(@Path("id") id: String): MediaDetails

    @GET("details/{id}/recommendations")
    suspend fun getRecommendations(@Path("id") id: String): List<MediaSummary>

    @GET("staff/{id}")
    suspend fun getStaffDetails(@Path("id") id: String): StaffDetails

    @GET("resolve-streams")
    suspend fun resolveStreams(
        @Query("subjectId") subjectId: String,
        @Query("detailPath") detailPath: String,
        @Query("se") se: Int,
        @Query("ep") ep: Int
    ): ResolveStreamsResponse

    @Headers(
        "Accept: application/json",
        "lang: en",
        "versioncode: 11",
        "clienttype: 0",
        "x-client-info: {\"timezone\":\"Asia/Calcutta\"}",
        "User-Agent: Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36"
    )
    @GET("https://movie-box.co/wefeed-h5api-bff/subject/play")
    suspend fun getMovieBoxPlay(
        @Header("Referer") referer: String,
        @Header("Authorization") authorization: String,
        @Header("Cookie") cookie: String,
        @Query("subjectId") subjectId: String,
        @Query("detailPath") detailPath: String,
        @Query("se") se: Int,
        @Query("ep") ep: Int
    ): MovieBoxPlayResponse

    // --- Official Live BFF Endpoints (from USER REQUEST) ---

    @POST("subject/search-suggest")
    suspend fun searchSuggest(
        @Body request: SearchSuggestRequest
    ): SearchSuggestResponse

    @POST("subject/search")
    suspend fun search(
        @Body request: SearchRequest
    ): SearchResponse

    @POST("subject/everyone-search")
    suspend fun getEveryoneSearch(
        @Body request: EveryoneSearchRequest
    ): Map<String, Any>

    @GET("subject/play")
    suspend fun getPlayBff(
        @Query("mediaId") mediaId: String,
        @Query("episodeId") episodeId: String? = null
    ): Map<String, Any>

    @GET("subject/caption")
    suspend fun getCaptionBff(@Query("mediaId") mediaId: String): Map<String, Any>

    @GET("subject/detail-rec")
    suspend fun getRecommendationsBff(@Query("mediaId") mediaId: String): Map<String, Any>

    @GET("detail")
    suspend fun getLiveDetails(@Query("subjectId") subjectId: String): DetailResponse

    @GET("subject/detail-rec")
    suspend fun getLiveRecommendations(
        @Query("subjectId") subjectId: String,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 18
    ): RecResponse

    @GET("tab/get-bottom-tab-list")
    suspend fun getBottomTabList(): Map<String, Any>

    @GET("webConfig")
    suspend fun getWebConfig(): Map<String, Any>

    @GET("detail")
    suspend fun getDetailBffLegacy(@Query("id") id: String): Map<String, Any>

    @GET("subject/details")
    suspend fun getDetailsBff(@Query("id") id: String): Map<String, Any>

    @GET("subject/episodes")
    suspend fun getEpisodesBff(
        @Query("mediaId") mediaId: String,
        @Query("seasonNo") seasonNo: Int? = null
    ): Map<String, Any>

    @GET("home")
    suspend fun getHome(
        @Query("host") host: String = "movie-box.co"
    ): HomeResponse

    @GET("home-extra")
    suspend fun getHomeExtra(): Map<String, Any>

    @GET("category")
    suspend fun getCategoryList(): Map<String, Any>

    @GET("client-info")
    suspend fun getClientInfo(): Map<String, Any>

    @GET("staff/staff-related")
    suspend fun getStaffRelatedBff(@Query("id") id: String): Map<String, Any>

    @GET("staff/subject-list")
    suspend fun getStaffSubjectsBff(@Query("id") id: String): Map<String, Any>

    @GET("subject/movie")
    suspend fun getMovieBff(@Query("id") id: String): Map<String, Any>

    @GET("ranking-list/content")
    suspend fun getRankingContent(): Map<String, Any>

    @POST("subject/filter")
    suspend fun getSubjectFilter(
        @Body request: FilterRequest
    ): Map<String, Any>
}
