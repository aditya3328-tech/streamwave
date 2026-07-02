package com.example.data.repository

import android.util.Log
import com.example.data.local.MovieDao
import com.example.data.model.HomeSection
import com.example.data.model.movie.MovieTabUiData
import com.example.data.remote.MovieTabApi
import com.example.data.remote.MovieTabRetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MovieTabRepository(
    private val api: MovieTabApi = MovieTabRetrofitClient.api,
    private val movieDao: MovieDao? = null,
) {
    val watchHistory = movieDao?.getWatchHistory()

    suspend fun getMovieTabData(): MovieTabUiData = coroutineScope {
        val operatingDeferred = async { api.getTabOperating() }
        val trendingDeferred = async { api.getTrendingMovies() }

        val operatingResponse = operatingDeferred.await()
        val trendingResponse = trendingDeferred.await()

        val operatingSections = operatingResponse.data?.operatingList.orEmpty()
        val banners = operatingSections
            .firstOrNull { it.type == "BANNER" }
            ?.banner
            ?.items
            .orEmpty()

        val categories = operatingSections.filter { it.type == "FILTER" }
        val subjectMovieSections = operatingSections.filter { it.type == "SUBJECTS_MOVIE" }
        val trendingMovies = trendingResponse.data?.allItems.orEmpty().map { it.toSubject() }

        val sections = buildList {
            addAll(categories)
            if (trendingMovies.isNotEmpty()) {
                add(
                    HomeSection(
                        type = "SUBJECTS_MOVIE",
                        position = nextPositionAfter(categories),
                        title = "🔥 Trending Movies",
                        subjects = trendingMovies,
                        banner = null,
                        opId = "movie_tab_trending",
                        url = null,
                        liveList = emptyList(),
                        filters = emptyList(),
                        customData = null,
                        genreTopId = null,
                        detailPath = null,
                    ),
                )
            }
            addAll(subjectMovieSections)
        }

        Log.d(
            "MOVIE_TAB",
            "banners=${banners.size} categories=${categories.size} trending=${trendingMovies.size} sections=${sections.size}",
        )

        MovieTabUiData(banners = banners, sections = sections)
    }

    private fun nextPositionAfter(sections: List<HomeSection>): Int =
        (sections.maxOfOrNull { it.position } ?: 0) + 1
}
