package com.example.movies.repository

import com.example.data.model.BannerItem
import com.example.data.model.HomeSection
import com.example.data.model.Subject
import com.example.data.remote.ApiService

class MovieTabRepository(
    private val apiService: ApiService?
) {
    suspend fun getMovieTabSections(): List<HomeSection> {
        return try {
            val response = apiService?.getMovieTabHome()
            response?.getOperatingList() ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("MOVIE_TAB_API", "Error fetching movie tab sections", e)
            emptyList()
        }
    }

    suspend fun getTrendingMovies(): List<Subject> {
        return try {
            val response = apiService?.getTrendingMovies()
            response?.getSubjectList() ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("MOVIE_TAB_API", "Error fetching trending movies", e)
            emptyList()
        }
    }
}
