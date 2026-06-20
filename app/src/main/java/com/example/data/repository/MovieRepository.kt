package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

class MovieRepository(
    private val apiService: ApiService?,
    private val movieDao: MovieDao
) {
    // Watchlist persistence
    val watchlist: Flow<List<WatchlistItem>> = movieDao.getWatchlist()
    suspend fun addToWatchlist(item: WatchlistItem) = movieDao.addToWatchlist(item)
    suspend fun removeFromWatchlist(mediaId: String) = movieDao.removeFromWatchlist(mediaId)
    fun isInWatchlist(mediaId: String): Flow<Boolean> = movieDao.isInWatchlist(mediaId)

    // Favorites persistence
    val favorites: Flow<List<FavoriteItem>> = movieDao.getFavorites()
    suspend fun addToFavorites(item: FavoriteItem) = movieDao.addToFavorites(item)
    suspend fun removeFromFavorites(mediaId: String) = movieDao.removeFromFavorites(mediaId)
    fun isFavorite(mediaId: String): Flow<Boolean> = movieDao.isFavorite(mediaId)

    // History persistence
    val watchHistory: Flow<List<WatchHistoryItem>> = movieDao.getWatchHistory()
    suspend fun saveWatchHistory(item: WatchHistoryItem) = movieDao.saveWatchHistory(item)
    suspend fun deleteWatchHistory(mediaId: String) = movieDao.deleteWatchHistory(mediaId)
    suspend fun getWatchHistoryItem(mediaId: String): WatchHistoryItem? = movieDao.getWatchHistoryItem(mediaId)

    // Remote data functions with fallback
    suspend fun getHomeSections(): List<HomeSection> {
        return try {
            val response = apiService?.getHome("movie-box.co")
            android.util.Log.d("HOME_API", "Raw Home response size: " + (response?.data?.operatingList?.size ?: 0))
            val sections = response?.data?.operatingList ?: emptyList()
            android.util.Log.d("HOME_SECTIONS", "Sections keys: " + sections.map { it.type }.toString())
            android.util.Log.d("HOME_COUNT", "Section count: " + sections.size.toString())
            sections
        } catch (e: Exception) {
            android.util.Log.e("HOME_API", "Error fetching home sections", e)
            emptyList()
        }
    }

    suspend fun getBannerItems(): List<BannerItem> {
        return try {
            val response = apiService?.getHome("movie-box.co")
            android.util.Log.d("HOME_API", "Raw Banner response: " + response.toString())
            val sections = response?.data?.operatingList ?: emptyList()
            val bannerSection = sections.find { it.type == "BANNER" }
            val banners = bannerSection?.banner?.items ?: emptyList()
            android.util.Log.d("BANNER_COUNT", "Banner count: " + banners.size)
            banners
        } catch (e: Exception) {
            android.util.Log.e("HOME_API", "Error fetching banner items", e)
            emptyList()
        }
    }

    suspend fun getHomeFeed(): List<MediaSummary> {
        return try {
            apiService?.getHomeFeed() ?: getMockFeed()
        } catch (e: Exception) {
            getMockFeed()
        }
    }

    suspend fun getTrending(): List<MediaSummary> {
        return try {
            apiService?.getTrending() ?: getMockTrending()
        } catch (e: Exception) {
            getMockTrending()
        }
    }

    suspend fun getPopularMovies(): List<MediaSummary> {
        return try {
            apiService?.getPopularMovies() ?: getMockMovies()
        } catch (e: Exception) {
            getMockMovies()
        }
    }

    suspend fun getPopularTVShows(): List<MediaSummary> {
        return try {
            apiService?.getPopularTVShows() ?: getMockTVShows()
        } catch (e: Exception) {
            getMockTVShows()
        }
    }

    suspend fun getRecentlyAdded(): List<MediaSummary> {
        return getMockRecentlyAdded()
    }

    suspend fun getTopRated(): List<MediaSummary> {
        return getMockTopRated()
    }

    suspend fun getAnimationFeed(): List<MediaSummary> {
        return getMockAnimation()
    }

    suspend fun getGenreMedia(genreName: String): List<MediaSummary> {
        return getMockByGenre(genreName)
    }

    suspend fun getRankings(): List<MediaSummary> {
        return try {
            apiService?.getRankings() ?: getMockRankings()
        } catch (e: Exception) {
            getMockRankings()
        }
    }

    suspend fun search(query: String): SearchResponse {
        return apiService?.search(SearchRequest(keyword = query, page = 1, perPage = 28, subjectType = 0)) ?: throw IllegalStateException("API service is not initialized")
    }

    suspend fun searchSuggest(query: String): SearchSuggestionResponse {
        return apiService?.searchSuggest(SearchSuggestRequest(keyword = query)) ?: throw IllegalStateException("API service is not initialized")
    }

    suspend fun getTrendingSearches(): List<String> {
        val service = apiService ?: return listOf("Action", "Comedy", "Sci-Fi", "Love", "Drama", "Fantasy", "Adventure")
        return try {
            val response = service.getEveryoneSearch(EveryoneSearchRequest(page = 1, perPage = 20))
            val items = response["data"]
            val list = if (items is Map<*, *>) {
                items["items"] ?: items["list"] ?: emptyList<Any>()
            } else if (items is List<*>) {
                items
            } else {
                emptyList<Any>()
            }

            val resultList = mutableListOf<String>()
            if (list is List<*>) {
                for (it in list) {
                    if (it is Map<*, *>) {
                        val word = (it["title"] ?: it["word"] ?: it["keyword"] ?: "").toString().trim()
                        if (word.isNotEmpty()) {
                            resultList.add(word)
                        }
                    } else if (it is String) {
                        resultList.add(it)
                    }
                }
            }
            if (resultList.isNotEmpty()) resultList else listOf("Action", "Comedy", "Sci-Fi", "Love", "Drama", "Fantasy", "Adventure")
        } catch (e: Exception) {
            android.util.Log.e("TRENDING_API", "Error getting trending suggestions, using custom local list fallback", e)
            listOf("Action", "Comedy", "Sci-Fi", "Love", "Drama", "Fantasy", "Adventure")
        }
    }

    suspend fun getDetails(id: String): MediaDetails {
        val summaryInPool = itemsPool.find { it.id == id }
        if (summaryInPool != null || id.all { !it.isDigit() }) {
            return getMockDetails(id)
        }

        return try {
            val service = apiService ?: throw Exception("ApiService is null")
            val response = service.getLiveDetails(id)
            
            // Print complete parsed Detail object in Logcat (Task 9)
            android.util.Log.d("ParsedDetail", "Parsed DetailResponse: $response")
            
            if (response.code == 0 && response.data != null) {
                val d = response.data
                val s = d.subject
                
                val imdbRating = s.imdbRatingValue ?: ""
                val trailerUrl = s.trailer?.videoAddress?.url ?: ""
                val trailerCover = s.trailer?.cover?.url ?: ""
                
                // Add debug logs (Task 3)
                android.util.Log.d("DETAIL_IMDB", imdbRating)
                android.util.Log.d("DETAIL_TRAILER_URL", trailerUrl)
                android.util.Log.d("DETAIL_TRAILER_COVER", trailerCover)

                val mappedEpisodes = mutableListOf<EpisodeSummary>()
                d.resource?.seasons?.forEach { season ->
                    val seNum = season.se ?: 1
                    val maxEp = season.maxEp ?: 1
                    for (epNum in 1..maxEp) {
                        mappedEpisodes.add(
                            EpisodeSummary(
                                id = "ep_${seNum}_${epNum}",
                                number = epNum,
                                title = "Episode $epNum",
                                overview = "Season $seNum, Episode $epNum",
                                thumbnailPath = s.cover?.url ?: "",
                                runtime = "45m",
                                streamUrl = ""
                            )
                        )
                    }
                }

                MediaDetails(
                    id = s.subjectId,
                    title = s.title,
                    posterPath = s.cover?.url ?: "",
                    backdropPath = s.trailer?.cover?.url ?: s.cover?.url ?: "",
                    mediaType = if (s.subjectType == 1) "movie" else "tv",
                    rating = imdbRating.toDoubleOrNull() ?: 0.0,
                    year = s.releaseDate?.take(4) ?: "",
                    country = d.countryName ?: "",
                    language = d.subtitles?.split(",")?.firstOrNull() ?: "",
                    genres = s.genre?.split(",") ?: emptyList(),
                    runtime = "${(s.duration ?: 0) / 60} min",
                    description = s.description ?: "",
                    cast = d.stars?.map { star ->
                        StaffSummary(
                            id = star.staffId,
                            name = star.name,
                            role = "Actor",
                            character = star.character,
                            profilePath = star.avatarUrl ?: ""
                        )
                    } ?: emptyList(),
                    crew = emptyList(),
                    trailerUrl = if (trailerUrl.isNotEmpty()) trailerUrl else null,
                    episodes = mappedEpisodes,
                    stars = d.stars ?: emptyList(),
                    dubs = d.dubs ?: emptyList(),
                    seasons = d.resource?.seasons ?: emptyList(),
                    comments = d.postList?.items ?: emptyList(),
                    trailerVideoAddressUrl = if (trailerUrl.isNotEmpty()) trailerUrl else null,
                    detailPath = s.detailPath
                )
            } else {
                getMockDetails(id)
            }
        } catch (e: Exception) {
            android.util.Log.e("DETAIL_API", "Failed to retrieve live details for $id, falling back to mock details", e)
            getMockDetails(id)
        }
    }

    suspend fun getRecommendations(id: String): List<MediaSummary> {
        val service = apiService ?: throw Exception("ApiService is null")
        val response = service.getLiveRecommendations(id, page = 1, perPage = 18)
        if (response.code == 0 && response.data?.items != null) {
            return response.data.items.map { item ->
                MediaSummary(
                    id = item.subjectId,
                    title = item.title,
                    posterPath = item.cover?.url ?: "",
                    backdropPath = item.cover?.url ?: "",
                    mediaType = if (item.subjectType == 1) "movie" else "tv",
                    rating = 0.0,
                    year = item.releaseDate?.take(4) ?: "",
                    genres = item.genre?.split(",") ?: emptyList()
                )
            }
        }
        return emptyList()
    }

    suspend fun getStaffDetails(id: String): StaffDetails {
        return try {
            apiService?.getStaffDetails(id) ?: getMockStaffDetails(id)
        } catch (e: Exception) {
            getMockStaffDetails(id)
        }
    }

    suspend fun getPlaybackDetails(mediaId: String, episodeId: String?): StreamPlayback {
        // HARD GUARD: Never silently serve mock content for real API calls.
        // If a non-production ID somehow reaches here, throw — do not play demo videos.
        if (itemsPool.any { it.id == mediaId } || mediaId.all { !it.isDigit() }) {
            android.util.Log.e("PLAYBACK_API", "BLOCKED: mediaId='$mediaId' matched mock pool or contains no digits. This is a dev-only ID. Real movies have numeric subjectIds.")
            throw IOException("Invalid subject ID '$mediaId'. Navigation passed a non-production ID.")
        }

        try {
            val details = getDetails(mediaId)

            // Use the MOVIE's own detailPath from the API — NOT dub/star paths
            var cleanPath = details.detailPath ?: ""
            if (cleanPath.isNotEmpty()) {
                // Strip any leading path segments, keep only the slug
                cleanPath = cleanPath.substringAfterLast("/")
            }

            // Only fall back to title slug if the API truly didn't return detailPath
            if (cleanPath.isEmpty() && details.title.isNotEmpty()) {
                android.util.Log.w("DETAIL_PATH", "Movie detailPath was null from API, generating slug from title — playback may fail")
                val titleSlug = details.title.lowercase()
                    .replace(Regex("[^a-z0-9\\s-]"), "")
                    .replace(Regex("\\s+"), "-")
                    .trim('-')
                cleanPath = if (titleSlug.isNotEmpty()) "$titleSlug-$mediaId" else mediaId
            }

            val isMovie = details.mediaType == "movie"
            val se: Int
            val ep: Int
            if (isMovie) {
                se = 0
                ep = 0
            } else {
                if (episodeId == null) {
                    se = 1
                    ep = 1
                } else {
                    val parts = episodeId.split("_")
                    if (parts.size >= 3 && parts[0] == "ep") {
                        se = parts[1].toIntOrNull() ?: 1
                        ep = parts[2].toIntOrNull() ?: 1
                    } else {
                        se = 1
                        ep = 1
                    }
                }
            }

            val service = apiService ?: throw Exception("ApiService is null")

            val referer = "https://movie-box.co/movies/$cleanPath?id=$mediaId&type=/movie/detail&detailSe=&detailEp=&lang=en"
            val playUrl = "https://movie-box.co/wefeed-h5api-bff/subject/play?subjectId=$mediaId&se=$se&ep=$ep&detailPath=$cleanPath"

            // Debugging Requirements: Before every request print:
            android.util.Log.d("PLAY_PARAMS", "subjectId=$mediaId, se=$se, ep=$ep")
            android.util.Log.d("DETAIL_PATH", cleanPath)
            android.util.Log.d("SUBJECT_ID", mediaId)
            android.util.Log.d("DETAIL_PATH_SOURCE", if (details.detailPath != null) "API (movie's own)" else "GENERATED (slug fallback)")
            android.util.Log.d("SEASON", se.toString())
            android.util.Log.d("EPISODE", ep.toString())
            android.util.Log.d("PLAY_URL", playUrl)

            var streams = emptyList<com.example.data.model.ResolveStreamItem>()
            var captions = emptyList<com.example.data.model.ResolveCaptionItem>()

            // 1. Call MovieBox subject/play — the only real stream source
            try {
                android.util.Log.d("PLAY_API_REQUEST", ">>> GET movie-box.co/subject/play subjectId=$mediaId detailPath=$cleanPath se=$se ep=$ep")
                val token =
                    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjg5NDQ2ODQzMjM3MzI2MDg1MTIsImF0cCI6MywiZXh0IjoiMTc4MDI4ODA4MiIsImV4cCI6MTc4ODA2NDA4MiwiaWF0IjoxNzgwMjg3NzgyfQ.30FAw16jFMuEHeUfjmSN1atbeYSZUn4QHFIxp3repbI"

                android.util.Log.e(
                    "PLAY_REQUEST",
                    "subjectId=$mediaId se=$se ep=$ep detailPath=$cleanPath"
                )
                val movieBoxResponse = service.getMovieBoxPlay(
                    referer = referer,
                    authorization = "Bearer $token",
                    cookie = "token=$token",
                    subjectId = mediaId,
                    detailPath = cleanPath,
                    se = se,
                    ep = ep
                )

                android.util.Log.e(
                    "PLAY_RESPONSE",
                    "streams=${movieBoxResponse.data?.streams?.size}"
                )

                android.util.Log.d("MOVIEBOX_RESPONSE", "code=${movieBoxResponse.code}, message=${movieBoxResponse.message}")
                android.util.Log.d("STREAM_COUNT", (movieBoxResponse.data?.streams?.size ?: 0).toString())

                // Print raw parsed response for debugging
                android.util.Log.d("PLAY_RESPONSE", "code=${movieBoxResponse.code} message=${movieBoxResponse.message} data=${movieBoxResponse.data}")

                // Verify API-level success (code 0 = OK, anything else = server error)
                if (movieBoxResponse.code != null && movieBoxResponse.code != 0) {
                    android.util.Log.e("PLAY_API_ERROR", "MovieBox API returned error code=${movieBoxResponse.code} message=${movieBoxResponse.message}")
                    throw IOException("MovieBox API error: code=${movieBoxResponse.code}, message=${movieBoxResponse.message}")
                }

                streams = movieBoxResponse.data?.streams ?: emptyList()
                captions = movieBoxResponse.data?.captions ?: emptyList()

                android.util.Log.d("PLAY_STREAMS_RAW", "Raw streams from API: $streams")
            } catch (e: IOException) {
                // Re-throw IOExceptions (includes our code-check error above)
                android.util.Log.e("PLAY_API_ERROR", "MovieBox Play API IOException", e)
                throw e
            } catch (e: Exception) {
                android.util.Log.e("PLAY_API_ERROR", "MovieBox Play API unexpected error", e)
                throw IOException("Failed to call MovieBox play API: ${e.message}", e)
            }

            // Debugging Requirements: Print STREAM_COUNT
            android.util.Log.d("STREAM_COUNT", streams.size.toString())

            // Log requirements (Available Streams)
            android.util.Log.d("Available Streams", streams.toString())

            if (streams.isNotEmpty()) {
                // Stream Selection Logic: 1080 -> 720 -> 480 -> 360 -> first available
                val selectedStream = streams.find { it.getResolvedQuality().contains("1080") }
                    ?: streams.find { it.getResolvedQuality().contains("720") }
                    ?: streams.find { it.getResolvedQuality().contains("480") }
                    ?: streams.find { it.getResolvedQuality().contains("360") }
                    ?: streams.first()

                // Debugging Requirements: Print STREAM_URL
                android.util.Log.d("STREAM_URL", selectedStream.getResolvedUrl())
                // Log requirement (Selected Stream)
                android.util.Log.d("Selected Stream", selectedStream.toString())

                val sortedStreams = streams.sortedByDescending { stream ->
                    val qualityStr = stream.getResolvedQuality()
                    val numericPortion = qualityStr.filter { it.isDigit() }.toIntOrNull() ?: 0
                    numericPortion
                }

                return StreamPlayback(
                    mediaId = mediaId,
                    episodeId = episodeId,
                    streamUrl = selectedStream.getResolvedUrl(),
                    format = selectedStream.getResolvedFormat(),
                    qualities = sortedStreams.map { PlaybackQuality(label = it.getResolvedQuality(), url = it.getResolvedUrl()) },
                    audioTracks = listOf(AudioTrack(label = "Default", languageCode = "en")),
                    subtitles = captions.map { SubtitleTrack(label = it.getResolvedLabel(), url = it.getResolvedUrl(), languageCode = it.getResolvedLanguageCode()) }
                )
            } else {
                android.util.Log.e("PLAYBACK_API", "No streams found for subjectId=$mediaId, detailPath=$cleanPath, se=$se, ep=$ep")
                throw IOException("No playback streams available for this title. Please try again later.")
            }
        } catch (e: IOException) {
            android.util.Log.e("PLAYBACK_API", "Playback stream resolution failed", e)
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PLAYBACK_API", "Unexpected error resolving streams", e)
            throw IOException("Failed to load playback stream: ${e.message}", e)
        }
    }

    // --- High-Quality Curated Mock Data Engine ---
    private val itemsPool = listOf(
        MediaSummary(
            id = "dune-2",
            title = "Dune: Part Two",
            posterPath = "https://images.unsplash.com/photo-1547483238-f400e65ccd56?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1200",
            mediaType = "movie",
            rating = 8.9,
            year = "2024",
            genres = listOf("Sci-Fi", "Adventure", "Drama")
        ),
        MediaSummary(
            id = "spiderman-verse",
            title = "Spider-Man: Across the Spider-Verse",
            posterPath = "https://images.unsplash.com/photo-1635805737707-575885ab0820?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=1200",
            mediaType = "movie",
            rating = 9.0,
            year = "2023",
            genres = listOf("Animation", "Action", "Sci-Fi")
        ),
        MediaSummary(
            id = "demon-slayer",
            title = "Demon Slayer: Kimetsu no Yaiba",
            posterPath = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=1200",
            mediaType = "anime",
            rating = 8.7,
            year = "2019",
            genres = listOf("Animation", "Action", "Fantasy")
        ),
        MediaSummary(
            id = "oppenheimer",
            title = "Oppenheimer",
            posterPath = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1200",
            mediaType = "movie",
            rating = 8.8,
            year = "2023",
            genres = listOf("Drama", "History", "Biography")
        ),
        MediaSummary(
            id = "interstellar",
            title = "Interstellar",
            posterPath = "https://images.unsplash.com/photo-1419242902214-272b3f66ee7a?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?q=80&w=1200",
            mediaType = "movie",
            rating = 8.6,
            year = "2014",
            genres = listOf("Sci-Fi", "Adventure", "Drama")
        ),
        MediaSummary(
            id = "jujutsu-kaisen",
            title = "Jujutsu Kaisen",
            posterPath = "https://images.unsplash.com/photo-1541562232579-512a21360020?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1560942485-b2a11cc13456?q=80&w=1200",
            mediaType = "anime",
            rating = 8.6,
            year = "2020",
            genres = listOf("Animation", "Action", "Horror")
        ),
        MediaSummary(
            id = "stranger-things",
            title = "Stranger Things",
            posterPath = "https://images.unsplash.com/photo-1509281373149-e957c6296406?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=1200",
            mediaType = "tv",
            rating = 8.7,
            year = "2016",
            genres = listOf("Sci-Fi", "Drama", "Horror")
        ),
        MediaSummary(
            id = "frieren",
            title = "Frieren: Beyond Journey's End",
            posterPath = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?q=80&w=1200",
            mediaType = "anime",
            rating = 9.1,
            year = "2023",
            genres = listOf("Animation", "Fantasy", "Drama")
        ),
        MediaSummary(
            id = "the-last-of-us",
            title = "The Last of Us",
            posterPath = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1200",
            mediaType = "tv",
            rating = 8.8,
            year = "2023",
            genres = listOf("Action", "Drama", "Sci-Fi")
        ),
        MediaSummary(
            id = "dark-knight",
            title = "The Dark Knight",
            posterPath = "https://images.unsplash.com/photo-1509281373149-e957c6296406?q=80&w=600",
            backdropPath = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=1200",
            mediaType = "movie",
            rating = 9.0,
            year = "2008",
            genres = listOf("Action", "Crime", "Drama")
        )
    )

    suspend fun getFilteredSubjects(type: Int, genre: String, country: String, year: String, language: String, sortBy: String): List<MediaSummary> {
        val reqGenre = if (genre == "All") null else genre
        val finalGenre = if (type == 3) "Animation" else reqGenre
        val reqCountry = if (country == "All") null else country
        val reqYear = if (year == "All") null else year
        val reqLanguage = if (language == "All") null else language
        val reqSort = if (sortBy == "Rating") "Rating" else if (sortBy == "Newest") "Newest" else "ForYou"

        val filterRequest = FilterRequest(
            subjectType = type,
            genre = finalGenre,
            country = reqCountry,
            year = reqYear,
            language = reqLanguage,
            sort = reqSort,
            page = 1,
            perPage = 20
        )

        // 6. FILTER_REQUEST Log
        android.util.Log.d("FILTER_REQUEST", "Request parameters: $filterRequest")

        return try {
            val response = apiService?.getSubjectFilter(filterRequest)
            
            // 6. FILTER_RESPONSE Log
            android.util.Log.d("FILTER_RESPONSE", "Response keys: ${response?.keys} | info: ${(response?.get("data") as? Map<*, *>)?.keys}")

            val subjectsList = mutableListOf<MediaSummary>()
            val data = response?.get("data")
            val list = if (data is Map<*, *>) {
                data["subjects"] ?: data["list"] ?: emptyList<Any>()
            } else if (data is List<*>) {
                data
            } else {
                emptyList<Any>()
            }

            if (list is List<*>) {
                for (item in list) {
                    if (item is Map<*, *>) {
                        val id = (item["subjectId"] ?: item["id"] ?: "").toString()
                        val title = (item["title"] ?: item["name"] ?: "").toString()
                        val cover = item["cover"]
                        val posterPath = if (cover is Map<*, *>) {
                            (cover["url"] ?: "").toString()
                        } else {
                            (cover ?: "").toString()
                        }
                        val stills = item["stills"]
                        val backdropPath = if (stills is Map<*, *>) {
                            (stills["url"] ?: "").toString()
                        } else {
                            (stills ?: "").toString()
                        }
                        val ratingStr = (item["imdbRatingValue"] ?: item["rating"] ?: "0.0").toString()
                        val yearVal = (item["releaseDate"] ?: item["year"] ?: "").toString().take(4)
                        val genreStr = (item["genre"] ?: "").toString()
                        val genresList = if (genreStr.isNotEmpty()) genreStr.split(",") else emptyList()

                        if (id.isNotEmpty() && title.isNotEmpty()) {
                            subjectsList.add(
                                MediaSummary(
                                    id = id,
                                    title = title,
                                    posterPath = posterPath,
                                    backdropPath = backdropPath,
                                    mediaType = if (type == 1) "movie" else if (type == 2) "tv" else "anime",
                                    rating = ratingStr.toDoubleOrNull() ?: 0.0,
                                    year = yearVal,
                                    genres = genresList
                                )
                            )
                        }
                    }
                }
            }

            // 6. MOVIES_COUNT Log
            android.util.Log.d("MOVIES_COUNT", "Parsed movies count from API: ${subjectsList.size}")
            subjectsList
        } catch (e: Exception) {
            android.util.Log.e("FILTER_API", "Error in getFilteredSubjects, returning empty results as requested", e)
            emptyList()
        }
    }

    private fun getMockFeed() = itemsPool
    private fun getMockTrending() = itemsPool.shuffled()
    private fun getMockMovies() = itemsPool.filter { it.mediaType == "movie" }
    private fun getMockTVShows() = itemsPool.filter { it.mediaType == "tv" }
    private fun getMockRecentlyAdded() = itemsPool.sortedByDescending { it.year }
    private fun getMockTopRated() = itemsPool.sortedByDescending { it.rating }
    private fun getMockAnimation() = itemsPool.filter { it.mediaType == "anime" }
    private fun getMockRankings() = itemsPool.sortedByDescending { it.rating }

    private fun getMockByGenre(genre: String): List<MediaSummary> {
        return itemsPool.filter { it.genres.contains(genre) }
    }



    private fun getMockDetails(id: String): MediaDetails {
        val summary = itemsPool.find { it.id == id } ?: itemsPool[0]
        
        // Define mock episodes if TV/Anime
        val episodes = if (summary.mediaType == "tv" || summary.mediaType == "anime") {
            List(12) { i ->
                EpisodeSummary(
                    id = "${summary.id}-ep-${i + 1}",
                    number = i + 1,
                    title = "Episode ${i + 1}: The Great Journey Begins",
                    overview = "As fate moves forward, our characters must face new hurdles and decide how they will forge their destiny together under the heavy tides of history.",
                    thumbnailPath = summary.backdropPath,
                    runtime = "24m",
                    streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
                )
            }
        } else {
            emptyList()
        }

        return MediaDetails(
            id = summary.id,
            title = summary.title,
            posterPath = summary.posterPath,
            backdropPath = summary.backdropPath,
            mediaType = summary.mediaType,
            rating = summary.rating,
            year = summary.year,
            country = if (summary.mediaType == "anime") "Japan" else "USA",
            language = if (summary.mediaType == "anime") "Japanese" else "English",
            genres = summary.genres,
            runtime = if (summary.mediaType == "movie") "2h 43m" else "12 Episodes",
            description = "In a vast universe filled with mystery and grand schemes, remarkable individuals find themselves bound together in an overarching legend. Confronting cosmic forces and complex human dynamics, they embark on a journey that will forever change the path of history, demonstrating the raw resilience of courage.",
            cast = listOf(
                StaffSummary("staff-timothee", "Timothée Chalamet", "Actor", "Paul Atreides", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=300"),
                StaffSummary("staff-zendaya", "Zendaya", "Actor", "Chani", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=300"),
                StaffSummary("staff-cillian", "Cillian Murphy", "Actor", "J. Robert Oppenheimer", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=300")
            ),
            crew = listOf(
                StaffSummary("staff-nolan", "Christopher Nolan", "Director", null, "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=300"),
                StaffSummary("staff-denis", "Denis Villeneuve", "Director", null, "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?q=80&w=300")
            ),
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            episodes = episodes
        )
    }

    private fun getMockRecommendations(id: String): List<MediaSummary> {
        return itemsPool.filter { it.id != id }.take(5)
    }

    private fun getMockStaffDetails(id: String): StaffDetails {
        val name = when(id) {
            "staff-timothee" -> "Timothée Chalamet"
            "staff-zendaya" -> "Zendaya"
            "staff-cillian" -> "Cillian Murphy"
            "staff-nolan" -> "Christopher Nolan"
            else -> "Denis Villeneuve"
        }
        val pPath = when(id) {
            "staff-timothee" -> "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=300"
            "staff-zendaya" -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=300"
            "staff-cillian" -> "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=300"
            "staff-nolan" -> "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=300"
            else -> "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?q=80&w=300"
        }
        return StaffDetails(
            id = id,
            name = name,
            biography = "$name is an acclaimed actor and filmmaker known for bringing profound intensity and deep emotional range to screen. Born in New York and having studied acting, this artist has broken benchmarks and worked with top visionaries to secure an indelible spot in modern visual arts history.",
            profilePath = pPath,
            knownFor = "Directing / Method Acting",
            filmography = itemsPool.shuffled().take(4)
        )
    }

    private fun getMockPlayback(mediaId: String, episodeId: String?): StreamPlayback {
        val streamToUse = if (mediaId == "demon-slayer" || mediaId == "jujutsu-kaisen" || mediaId == "frieren") {
            // Sintel (Anime/Cartoony vibe)
            "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8"
        } else {
            // Tears of steel HLS
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
        }
        return StreamPlayback(
            mediaId = mediaId,
            episodeId = episodeId,
            streamUrl = streamToUse,
            format = "hls",
            qualities = listOf(
                PlaybackQuality("1080p (HQ)", streamToUse),
                PlaybackQuality("720p", streamToUse),
                PlaybackQuality("480p (Mobile)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
            ),
            audioTracks = listOf(
                AudioTrack("English (Original)", "en"),
                AudioTrack("Japanese (Sub)", "ja")
            ),
            subtitles = listOf(
                SubtitleTrack("English", "https://example.com/sub_en.vtt", "en"),
                SubtitleTrack("Spanish", "https://example.com/sub_es.vtt", "es")
            )
        )
    }
}
