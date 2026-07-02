package com.example.data.repository

import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MovieRepository(
    private val apiService: ApiService?,
    private val streamRepository: StreamRepository,
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
                android.util.Log.d("DUBS", "Loaded ${s.dubs.size} languages")

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
                    dubs = s.dubs,
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

    // --- Staff Detail (Person Profile) screen ---
    // Defensive parsing: the upstream JSON field names are not guaranteed, so we probe
    // several likely keys for each value and degrade gracefully (never crash, empty on failure).

    suspend fun getStaffFilmographyPage(staffId: String, page: Int, perPage: Int = 18): StaffFilmographyPage {
        val service = apiService ?: return StaffFilmographyPage(emptyList(), 0, false, null)
        return try {
            val resp = service.getStaffSubjectList(staffId, page, perPage)
            val data = resp.asMapOrNull("data") ?: resp
            val rawItems = data.firstList("items", "subjectList", "subjects", "list", "records")
            val items = rawItems.mapNotNull { (it as? Map<*, *>)?.let(::parseSubjectItem) }
                .filter { it.id.isNotBlank() }
            val total = data.firstInt("subjectNum", "total", "totalCount", "count")
                .let { if (it > 0) it else items.size }
            val profile = if (page == 1) {
                val staffMap = data.asMapOrNull("staff") ?: data.asMapOrNull("staffInfo") ?: data
                parseStaffProfile(staffMap, staffId, total)
            } else null
            val pager = data.asMapOrNull("pager") ?: data.asMapOrNull("page")
            val hasMore = data.firstBool("hasMore")
                ?: pager?.firstBool("hasMore")
                ?: (items.size >= perPage)
            StaffFilmographyPage(items, total, hasMore, profile)
        } catch (e: Exception) {
            Log.e("STAFF_API", "Filmography page $page failed for $staffId", e)
            StaffFilmographyPage(emptyList(), 0, false, null)
        }
    }

    suspend fun getStaffRelatedCelebrities(staffId: String): List<RelatedCelebrity> {
        val service = apiService ?: return emptyList()
        return try {
            val resp = service.getStaffRelatedList(staffId)
            val data = resp.asMapOrNull("data") ?: resp
            val raw = data.firstList("items", "list", "staffList", "relatedList", "staffs", "records")
            raw.mapNotNull { (it as? Map<*, *>)?.let(::parseRelated) }
                .filter { it.id.isNotBlank() && it.name.isNotBlank() }
        } catch (e: Exception) {
            Log.e("STAFF_API", "Related celebrities failed for $staffId", e)
            emptyList()
        }
    }

    private fun parseSubjectItem(m: Map<*, *>): MediaSummary {
        val poster = m.firstString("poster", "posterPath", "image", "cover2")
            .ifEmpty { m.asMapOrNull("cover")?.firstString("url") ?: "" }
        val rating = m.firstString("imdbRatingValue", "imdbRating", "rating")
        return MediaSummary(
            id = m.firstString("subjectId", "id"),
            title = m.firstString("title", "name"),
            posterPath = poster,
            backdropPath = poster,
            mediaType = if (m.firstInt("subjectType") == 1) "movie" else "tv",
            rating = rating.toDoubleOrNull() ?: 0.0,
            year = m.firstString("releaseDate", "year").take(4),
            genres = m.firstString("genre").split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )
    }

    private fun parseStaffProfile(m: Map<*, *>?, staffId: String, total: Int): StaffProfileInfo {
        val avatar = (m?.firstString("avatarUrl", "avatar", "image", "profilePath") ?: "")
            .ifEmpty { m?.asMapOrNull("cover")?.firstString("url") ?: "" }
        return StaffProfileInfo(
            id = staffId,
            name = m?.firstString("name", "staffName", "title") ?: "",
            avatarUrl = avatar,
            roles = buildRoles(m),
            biography = m?.firstString("description", "biography", "intro", "summary", "detail") ?: "",
            totalTitles = total
        )
    }

    private fun parseRelated(m: Map<*, *>): RelatedCelebrity {
        val avatar = m.firstString("avatarUrl", "avatar", "image", "profilePath")
            .ifEmpty { m.asMapOrNull("cover")?.firstString("url") ?: "" }
        return RelatedCelebrity(
            id = m.firstString("staffId", "id"),
            name = m.firstString("name", "staffName"),
            role = buildRoles(m).ifEmpty { "Actor" },
            avatarUrl = avatar
        )
    }

    private fun buildRoles(m: Map<*, *>?): String {
        if (m == null) return ""
        (m["departments"] as? List<*>)?.let { list ->
            val roles = list.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
            if (roles.isNotEmpty()) return roles.joinToString(" • ")
        }
        val single = m.firstString("department", "knownFor", "role", "character", "job")
        if (single.isNotEmpty()) return single
        return when (m.firstInt("staffType")) {
            1 -> "Actor"
            2 -> "Director"
            3 -> "Writer"
            4 -> "Producer"
            else -> ""
        }
    }

    // Map probing helpers (values come back as Double / String / Map / List from the JSON converter).
    private fun Map<*, *>.firstString(vararg keys: String): String {
        for (k in keys) {
            val v = this[k] ?: continue
            val s = v.toString().trim()
            if (s.isNotEmpty() && s != "null") return s
        }
        return ""
    }

    private fun Map<*, *>.firstInt(vararg keys: String): Int {
        for (k in keys) {
            when (val v = this[k]) {
                is Number -> return v.toInt()
                is String -> v.trim().toDoubleOrNull()?.let { return it.toInt() }
                else -> {}
            }
        }
        return 0
    }

    private fun Map<*, *>.firstBool(vararg keys: String): Boolean? {
        for (k in keys) {
            when (val v = this[k]) {
                is Boolean -> return v
                is String -> if (v.equals("true", true)) return true else if (v.equals("false", true)) return false
                else -> {}
            }
        }
        return null
    }

    private fun Map<*, *>.asMapOrNull(key: String): Map<*, *>? = this[key] as? Map<*, *>

    private fun Map<*, *>.firstList(vararg keys: String): List<*> {
        for (k in keys) {
            (this[k] as? List<*>)?.let { return it }
        }
        // Sometimes the list is nested one level deeper, e.g. data.subjectList.items
        for (k in keys) {
            val nested = this[k] as? Map<*, *> ?: continue
            (nested["items"] as? List<*> ?: nested["list"] as? List<*>)?.let { return it }
        }
        return emptyList<Any>()
    }

    private suspend fun resolveSeasonEpisode(mediaId: String, episodeId: String?): Pair<Int, Int> {
        val details = getDetails(mediaId)
        val isMovie = details.mediaType == "movie"
        if (isMovie) return Pair(0, 0)
        if (episodeId == null) return Pair(1, 1)
        val parts = episodeId.split("_")
        if (parts.size >= 3 && parts[0] == "ep") {
            return Pair(parts[1].toIntOrNull() ?: 1, parts[2].toIntOrNull() ?: 1)
        }
        return Pair(1, 1)
    }

    suspend fun getPlaybackUrl(mediaId: String, detailPath: String, season: Int, episode: Int): String? {
        // Direct upstream call — no custom backend proxy.
        // Calls h5-api.aoneroom.com/wefeed-h5api-bff/subject/play via StreamResolver → MovieBoxProvider.

        android.util.Log.e("PLAYBACK_API", "=== DIRECT UPSTREAM CALL ===")
        android.util.Log.e("PLAYBACK_API", "API Base: h5-api.aoneroom.com/wefeed-h5api-bff")
        android.util.Log.e("PLAYBACK_API", "Endpoint: GET /subject/play")
        android.util.Log.e("PLAYBACK_API", "SubjectId = $mediaId")
        android.util.Log.e("PLAYBACK_API", "DetailPath = $detailPath")
        android.util.Log.e("PLAYBACK_API", "Season = $season")
        android.util.Log.e("PLAYBACK_API", "Episode = $episode")

        val result: ResolvedStreamPayload
        try {
            Log.d("PLAYBACK_API", "Calling streamRepository.getStreams() → MovieBoxProvider → upstream API")
            result = streamRepository.getStreams(
                subjectId = mediaId,
                detailPath = detailPath,
                season = season,
                episode = episode
            )
            Log.e("PLAYBACK_API", "=== DIRECT RESPONSE ===")
            Log.e("PLAYBACK_API", "Success = ${result.success}")
            Log.e("PLAYBACK_API", "Streams count = ${result.streams.size}")
            Log.e("PLAYBACK_API", "Selected stream present = ${result.selectedStream != null}")
            // FLOW_TRACE LOG 4 & 5 — selected stream + URL at the repository boundary
            Log.e("FLOW_TRACE", "Repository received count = ${result.streams.size}")
            Log.e("FLOW_TRACE", "Selected stream = ${result.selectedStream}")
            Log.e("FLOW_TRACE", "Selected URL = ${result.selectedStream?.url}")
        } catch (e: UnknownHostException) {
            Log.e("PLAYBACK_API", "NETWORK ERROR: Upstream API unreachable (unknown host)")
            throw IOException("Upstream API unreachable", e)
        } catch (e: ConnectException) {
            Log.e("PLAYBACK_API", "NETWORK ERROR: Connection refused to upstream API")
            throw IOException("Unable to connect to upstream API", e)
        } catch (e: SocketTimeoutException) {
            Log.e("PLAYBACK_API", "NETWORK ERROR: Timeout connecting to upstream API")
            throw IOException("Upstream API timeout", e)
        } catch (e: IOException) {
            Log.e("PLAYBACK_API", "NETWORK ERROR: I/O Error - ${e.message}")
            throw IOException("Network error: ${e.message}", e)
        }

        if (!result.success) {
            Log.e("PLAYBACK_API", "VALIDATION ERROR: Upstream returned success=false")
            throw IOException("No playable stream available")
        }

        if (result.selectedStream != null) {
            val url = result.selectedStream.url
            if (url.isBlank()) {
                Log.e("PLAYBACK_API", "VALIDATION ERROR: selectedStream.url is empty")
                throw IOException("Selected stream URL is blank")
            }
            Log.e("PLAYBACK_API", "=== SELECTED STREAM ===")
            Log.e("PLAYBACK_API", "URL = $url")
            Log.e("PLAYBACK_API", "Quality = ${result.selectedStream.quality}")
            Log.e("PLAYBACK_API", "Type = ${result.selectedStream.type}")
            return url
        }

        if (result.streams.isNotEmpty()) {
            Log.w("PLAYBACK_API", "selectedStream is null, falling back to highest quality stream")
            val sorted = result.streams.sortedByDescending { it.numericQuality }
            val best = sorted.first()
            if (best.url.isBlank()) {
                Log.e("PLAYBACK_API", "VALIDATION ERROR: Best stream URL is empty")
                throw IOException("No valid stream URL found")
            }
            Log.e("PLAYBACK_API", "=== FALLBACK STREAM ===")
            Log.e("PLAYBACK_API", "URL = ${best.url}")
            Log.e("PLAYBACK_API", "Quality = ${best.quality}")
            return best.url
        }

        Log.e("PLAYBACK_API", "VALIDATION ERROR: No streams available in response")
        throw IOException("No playable stream found")
    }

    suspend fun getPlaybackDetails(mediaId: String, episodeId: String?): StreamPlayback {
        if (itemsPool.any { it.id == mediaId } || mediaId.all { !it.isDigit() }) {
            android.util.Log.e("PLAYBACK_API", "BLOCKED: mediaId='$mediaId' matched mock pool or contains no digits. This is a dev-only ID. Real movies have numeric subjectIds.")
            throw IOException("Invalid subject ID '$mediaId'. Navigation passed a non-production ID.")
        }

        try {
            val details = getDetails(mediaId)

            // PARITY — send the FULL detailPath to the upstream /subject/play call, exactly
            // like the website does (streamwave moviebox.js passes detailPath verbatim and uses
            // it to build the CDN Referer https://movie-box.co/movies/${detailPath}?id=${id}).
            // No truncation: MovieBox detailPaths contain no "/", and the full value is required.
            var cleanPath = details.detailPath ?: ""
            if (cleanPath.isEmpty() && details.title.isNotEmpty()) {
                android.util.Log.w("DETAIL_PATH", "Movie detailPath was null from API, generating slug from title")
                val titleSlug = details.title.lowercase()
                    .replace(Regex("[^a-z0-9\\s-]"), "")
                    .replace(Regex("\\s+"), "-")
                    .trim('-')
                cleanPath = if (titleSlug.isNotEmpty()) "$titleSlug-$mediaId" else mediaId
            }

            val (se, ep) = resolveSeasonEpisode(mediaId, episodeId)

            android.util.Log.d("PLAYBACK_API", "=== PLAYBACK DETAILS ===")
            android.util.Log.d("PLAYBACK_API", "SubjectId = $mediaId")
            android.util.Log.d("PLAYBACK_API", "Title = ${details.title}")
            android.util.Log.d("PLAYBACK_API", "MediaType = ${details.mediaType}")
            android.util.Log.d("PLAYBACK_API", "Season = $se")
            android.util.Log.d("PLAYBACK_API", "Episode = $ep")
            android.util.Log.d("PLAYBACK_API", "DetailPath = $cleanPath")

            val streamUrl = getPlaybackUrl(mediaId, cleanPath, se, ep)
                ?: throw IOException("Stream URL resolution failed: returned null")

            val format = if (streamUrl.contains(".m3u8", ignoreCase = true)) "hls" else "mp4"
            android.util.Log.d("PLAYBACK_API", "Stream format detected = $format")

            // ADDITIVE (UI only): expose all already-resolved qualities + existing-API captions.
            // These re-read the SAME cached resolution (cache hit, no extra resolve) and the
            // existing caption API — the resolution algorithm and streamUrl above are untouched.
            val qualities = getPlaybackQualities(mediaId, cleanPath, se, ep, streamUrl)
            val subtitles = getSubtitles(mediaId, cleanPath)
            android.util.Log.d("PLAYBACK_API", "Quality options = ${qualities.map { it.label }}")
            android.util.Log.d("PLAYBACK_API", "Subtitle tracks = ${subtitles.map { it.label }}")

            return StreamPlayback(
                mediaId = mediaId,
                episodeId = episodeId,
                streamUrl = streamUrl,
                format = format,
                qualities = qualities,
                audioTracks = listOf(AudioTrack(label = "Default", languageCode = "en")),
                subtitles = subtitles
            )
        } catch (e: IOException) {
            android.util.Log.e("PLAYBACK_API", "IO Error: ${e.message}")
            throw e
        } catch (e: Exception) {
            android.util.Log.e("PLAYBACK_API", "Unexpected error: ${e.javaClass.simpleName} - ${e.message}")
            throw IOException("Failed to load playback stream: ${e.message}", e)
        }
    }

    /**
     * ADDITIVE — enumerate every already-resolved stream as a selectable quality.
     * Re-reads streamRepository.getStreams(), which the resolver has just cached for this
     * subject/se/ep, so this is a cache hit (no extra network resolve) and the resolution
     * algorithm is NOT touched. Always returns at least the "Auto" entry pointing at the
     * exact URL ExoPlayer is already playing.
     */
    suspend fun getPlaybackQualities(
        subjectId: String,
        detailPath: String,
        season: Int,
        episode: Int,
        bestUrl: String
    ): List<PlaybackQuality> {
        val auto = PlaybackQuality(label = "Auto", url = bestUrl)
        return try {
            val result = streamRepository.getStreams(subjectId, detailPath, season, episode)
            val mapped = result.streams
                .filter { it.url.isNotBlank() }
                .sortedByDescending { it.numericQuality }
                .map { s ->
                    val label = if (s.numericQuality > 0) "${s.numericQuality}p"
                                else s.quality.ifBlank { "Source" }
                    PlaybackQuality(label = label, url = s.url)
                }
                .distinctBy { it.label }
            listOf(auto) + mapped
        } catch (e: Exception) {
            android.util.Log.w("PLAYBACK_API", "Quality enumeration failed: ${e.message}")
            listOf(auto)
        }
    }

    /**
     * ADDITIVE — fetch captions via the EXISTING (untouched) caption API and map them to
     * the UI's SubtitleTrack model. Best-effort: any failure yields an empty list so video
     * playback is never blocked by caption errors.
     */
    suspend fun getSubtitles(
        subjectId: String,
        detailPath: String
    ): List<SubtitleTrack> {
        return try {
            streamRepository.getCaptions(id = subjectId, subjectId = subjectId, detailPath = detailPath)
                .filter { it.url.isNotBlank() }
                .map { c ->
                    SubtitleTrack(
                        label = c.languageName.ifBlank { c.language.uppercase() },
                        url = c.url,
                        languageCode = c.language
                    )
                }
                .distinctBy { it.languageCode }
        } catch (e: Exception) {
            android.util.Log.w("PLAYBACK_API", "Caption fetch failed: ${e.message}")
            emptyList()
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

    // One page of Explore results plus pagination flags from data.pager.
    data class ExplorePage(
        val items: List<MediaSummary>,
        val hasMore: Boolean,
        val nextPage: Int
    )

    // Explore page (MovieBox-style) filtered subjects. "All" maps to empty string.
    // Parses data.items and data.pager.{hasMore,nextPage}. Reuses the shared
    // Retrofit/OkHttp interceptors (headers injected centrally — none hardcoded here).
    suspend fun getExploreSubjects(
        genre: String,
        country: String,
        year: String,
        page: Int,
        perPage: Int = 28
    ): ExplorePage {
        val request = ExploreFilterRequest(
            genre = if (genre == "All") "" else genre,
            country = if (country == "All") "" else country,
            year = if (year == "All") "" else year,
            page = page,
            perPage = perPage
        )

        android.util.Log.d("EXPLORE_REQUEST", "Request parameters: $request")

        return try {
            val response = apiService?.getExploreFilter(request)
            val data = response?.get("data") as? Map<*, *>
            val rawList = (data?.get("items") ?: data?.get("subjects") ?: data?.get("list")) as? List<*>
                ?: emptyList<Any>()

            val items = rawList.mapNotNull { item ->
                if (item !is Map<*, *>) return@mapNotNull null
                val id = (item["subjectId"] ?: item["id"] ?: "").toString()
                val title = (item["title"] ?: item["name"] ?: "").toString()
                if (id.isEmpty() || title.isEmpty()) return@mapNotNull null

                val cover = item["cover"]
                val posterPath = if (cover is Map<*, *>) (cover["url"] ?: "").toString() else (cover ?: "").toString()
                val stills = item["stills"]
                val backdropPath = if (stills is Map<*, *>) (stills["url"] ?: "").toString() else (stills ?: "").toString()
                val ratingStr = (item["imdbRatingValue"] ?: item["rating"] ?: "0.0").toString()
                val yearVal = (item["releaseDate"] ?: item["year"] ?: "").toString().take(4)
                val genreStr = (item["genre"] ?: "").toString()
                val subjectType = (item["subjectType"] as? Number)?.toInt()

                MediaSummary(
                    id = id,
                    title = title,
                    posterPath = posterPath,
                    backdropPath = backdropPath,
                    mediaType = if (subjectType == 1) "movie" else "tv",
                    rating = ratingStr.toDoubleOrNull() ?: 0.0,
                    year = yearVal,
                    genres = if (genreStr.isNotEmpty()) genreStr.split(",") else emptyList()
                )
            }

            val pager = data?.get("pager") as? Map<*, *>
            val hasMore = (pager?.get("hasMore") as? Boolean) ?: false
            val nextPage = (pager?.get("nextPage") as? Number)?.toInt() ?: (page + 1)

            android.util.Log.d("EXPLORE_RESPONSE", "items=${items.size} hasMore=$hasMore nextPage=$nextPage")
            ExplorePage(items, hasMore, nextPage)
        } catch (e: Exception) {
            android.util.Log.e("EXPLORE_API", "Error in getExploreSubjects", e)
            ExplorePage(emptyList(), false, page)
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
