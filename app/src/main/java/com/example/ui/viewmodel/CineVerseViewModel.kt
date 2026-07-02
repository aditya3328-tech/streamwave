package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteItem
import com.example.data.local.WatchHistoryItem
import com.example.data.local.WatchlistItem
import com.example.data.model.*
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    object Loading : DetailsUiState
    data class Success(val details: MediaDetails, val recommendations: List<MediaSummary>) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

sealed interface StaffUiState {
    object Loading : StaffUiState
    data class Success(val data: StaffPageData) : StaffUiState
    data class Error(val message: String) : StaffUiState
}

sealed interface PlaybackUiState {
    object Idle : PlaybackUiState
    object Loading : PlaybackUiState
    data class Success(val playback: StreamPlayback, val mediaDetails: MediaDetails, val recommendations: List<MediaSummary> = emptyList()) : PlaybackUiState
    data class Error(val message: String) : PlaybackUiState
}

class CineVerseViewModel(private val repository: MovieRepository) : ViewModel() {
    private var playbackJob: kotlinx.coroutines.Job? = null

    // Bottom Navigation Tab System
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    // Home feed states
    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    private val _homeSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val homeSections: StateFlow<List<HomeSection>> = _homeSections.asStateFlow()

    private val _bannerItems = MutableStateFlow<List<BannerItem>>(emptyList())
    val bannerItems: StateFlow<List<BannerItem>> = _bannerItems.asStateFlow()

    private val _heroBanner = MutableStateFlow<MediaSummary?>(null)
    val heroBanner: StateFlow<MediaSummary?> = _heroBanner.asStateFlow()

    private val _trending = MutableStateFlow<List<MediaSummary>>(emptyList())
    val trending: StateFlow<List<MediaSummary>> = _trending.asStateFlow()

    private val _popularMovies = MutableStateFlow<List<MediaSummary>>(emptyList())
    val popularMovies: StateFlow<List<MediaSummary>> = _popularMovies.asStateFlow()

    private val _popularTVShows = MutableStateFlow<List<MediaSummary>>(emptyList())
    val popularTVShows: StateFlow<List<MediaSummary>> = _popularTVShows.asStateFlow()

    private val _recentlyAdded = MutableStateFlow<List<MediaSummary>>(emptyList())
    val recentlyAdded: StateFlow<List<MediaSummary>> = _recentlyAdded.asStateFlow()

    private val _topRated = MutableStateFlow<List<MediaSummary>>(emptyList())
    val topRated: StateFlow<List<MediaSummary>> = _topRated.asStateFlow()

    private val _animationFeed = MutableStateFlow<List<MediaSummary>>(emptyList())
    val animationFeed: StateFlow<List<MediaSummary>> = _animationFeed.asStateFlow()

    // Movie filter selections
    private val _movieSelectedGenre = MutableStateFlow("All")
    val movieSelectedGenre: StateFlow<String> = _movieSelectedGenre.asStateFlow()

    private val _movieSelectedCountry = MutableStateFlow("All")
    val movieSelectedCountry: StateFlow<String> = _movieSelectedCountry.asStateFlow()

    private val _movieSelectedYear = MutableStateFlow("All")
    val movieSelectedYear: StateFlow<String> = _movieSelectedYear.asStateFlow()

    private val _movieSelectedLanguage = MutableStateFlow("All")
    val movieSelectedLanguage: StateFlow<String> = _movieSelectedLanguage.asStateFlow()

    private val _movieSelectedSortBy = MutableStateFlow("Popularity")
    val movieSelectedSortBy: StateFlow<String> = _movieSelectedSortBy.asStateFlow()

    private val _isMoviesLoading = MutableStateFlow(false)
    val isMoviesLoading: StateFlow<Boolean> = _isMoviesLoading.asStateFlow()

    private val _moviesList = MutableStateFlow<List<MediaSummary>>(emptyList())
    val moviesList: StateFlow<List<MediaSummary>> = _moviesList.asStateFlow()


    // Explore filter selections (formerly "TV Shows")
    private val _exploreSelectedGenre = MutableStateFlow("All")
    val exploreSelectedGenre: StateFlow<String> = _exploreSelectedGenre.asStateFlow()

    private val _exploreSelectedCountry = MutableStateFlow("All")
    val exploreSelectedCountry: StateFlow<String> = _exploreSelectedCountry.asStateFlow()

    private val _exploreSelectedYear = MutableStateFlow("All")
    val exploreSelectedYear: StateFlow<String> = _exploreSelectedYear.asStateFlow()

    private val _isExploreLoading = MutableStateFlow(false)
    val isExploreLoading: StateFlow<Boolean> = _isExploreLoading.asStateFlow()

    private val _isExploreLoadingMore = MutableStateFlow(false)
    val isExploreLoadingMore: StateFlow<Boolean> = _isExploreLoadingMore.asStateFlow()

    private val _exploreHasMore = MutableStateFlow(false)
    val exploreHasMore: StateFlow<Boolean> = _exploreHasMore.asStateFlow()

    private val _exploreList = MutableStateFlow<List<MediaSummary>>(emptyList())
    val exploreList: StateFlow<List<MediaSummary>> = _exploreList.asStateFlow()

    // Explore pagination bookkeeping
    private var explorePage: Int = 1
    private var exploreJob: Job? = null


    // Anime / Animation filter selections
    private val _animeSelectedCountry = MutableStateFlow("All")
    val animeSelectedCountry: StateFlow<String> = _animeSelectedCountry.asStateFlow()

    private val _animeSelectedYear = MutableStateFlow("All")
    val animeSelectedYear: StateFlow<String> = _animeSelectedYear.asStateFlow()

    private val _animeSelectedSortBy = MutableStateFlow("Popularity")
    val animeSelectedSortBy: StateFlow<String> = _animeSelectedSortBy.asStateFlow()

    private val _isAnimeLoading = MutableStateFlow(false)
    val isAnimeLoading: StateFlow<Boolean> = _isAnimeLoading.asStateFlow()

    private val _animeList = MutableStateFlow<List<MediaSummary>>(emptyList())
    val animeList: StateFlow<List<MediaSummary>> = _animeList.asStateFlow()


    // Legacy support
    val moviesFilterGenre: StateFlow<String> = _movieSelectedGenre
    val tvFilterGenre: StateFlow<String> = _exploreSelectedGenre


    fun applyMovieFilters(genre: String, country: String, year: String, language: String, sortBy: String) {
        _movieSelectedGenre.value = genre
        _movieSelectedCountry.value = country
        _movieSelectedYear.value = year
        _movieSelectedLanguage.value = language
        _movieSelectedSortBy.value = sortBy
        
        viewModelScope.launch {
            _isMoviesLoading.value = true
            try {
                _moviesList.value = repository.getFilteredSubjects(
                    type = 1,
                    genre = genre,
                    country = country,
                    year = year,
                    language = language,
                    sortBy = sortBy
                )
            } catch (e: Exception) {
                _moviesList.value = emptyList()
            } finally {
                _isMoviesLoading.value = false
            }
        }
    }

    fun resetMovieFilters() {
        applyMovieFilters("All", "All", "All", "All", "Popularity")
    }

    fun applyExploreFilters(genre: String, country: String, year: String) {
        _exploreSelectedGenre.value = genre
        _exploreSelectedCountry.value = country
        _exploreSelectedYear.value = year

        exploreJob?.cancel()
        exploreJob = viewModelScope.launch {
            _isExploreLoading.value = true
            explorePage = 1
            try {
                val page = repository.getExploreSubjects(genre, country, year, 1)
                _exploreList.value = page.items
                _exploreHasMore.value = page.hasMore
                explorePage = page.nextPage
            } catch (e: Exception) {
                _exploreList.value = emptyList()
                _exploreHasMore.value = false
            } finally {
                _isExploreLoading.value = false
            }
        }
    }

    fun loadMoreExplore() {
        if (_isExploreLoading.value || _isExploreLoadingMore.value || !_exploreHasMore.value) return
        viewModelScope.launch {
            _isExploreLoadingMore.value = true
            try {
                val page = repository.getExploreSubjects(
                    _exploreSelectedGenre.value,
                    _exploreSelectedCountry.value,
                    _exploreSelectedYear.value,
                    explorePage
                )
                _exploreList.value = _exploreList.value + page.items
                _exploreHasMore.value = page.hasMore
                explorePage = page.nextPage
            } catch (e: Exception) {
                _exploreHasMore.value = false
            } finally {
                _isExploreLoadingMore.value = false
            }
        }
    }

    fun resetExploreFilters() {
        applyExploreFilters("All", "All", "All")
    }

    fun applyAnimeFilters(country: String, year: String, sortBy: String) {
        _animeSelectedCountry.value = country
        _animeSelectedYear.value = year
        _animeSelectedSortBy.value = sortBy

        viewModelScope.launch {
            _isAnimeLoading.value = true
            try {
                _animeList.value = repository.getFilteredSubjects(
                    type = 3,
                    genre = "All",
                    country = country,
                    year = year,
                    language = "All",
                    sortBy = sortBy
                )
            } catch (e: Exception) {
                _animeList.value = emptyList()
            } finally {
                _isAnimeLoading.value = false
            }
        }
    }

    fun resetAnimeFilters() {
        applyAnimeFilters("All", "All", "Popularity")
    }

    fun setMoviesFilter(genre: String) {
        applyMovieFilters(genre, _movieSelectedCountry.value, _movieSelectedYear.value, _movieSelectedLanguage.value, _movieSelectedSortBy.value)
    }

    // Backward-compatible entry point used by the Home screen quick-filters.
    fun setTvFilter(genre: String) {
        applyExploreFilters(genre, _exploreSelectedCountry.value, _exploreSelectedYear.value)
    }

    // Room Persistent states (Zustand storage equivalent)
    val watchlist: StateFlow<List<WatchlistItem>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteItem>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchHistory: StateFlow<List<WatchHistoryItem>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Details page states
    private val _detailsUiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val detailsUiState: StateFlow<DetailsUiState> = _detailsUiState.asStateFlow()

    // Staff screen states
    private val _staffUiState = MutableStateFlow<StaffUiState>(StaffUiState.Loading)
    val staffUiState: StateFlow<StaffUiState> = _staffUiState.asStateFlow()

    // Staff filmography pagination bookkeeping
    private var staffId: String = ""
    private var staffPage: Int = 1
    private var staffHasMore: Boolean = false
    private var staffLoadingMore: Boolean = false
    private val STAFF_PER_PAGE = 18

    // Player screen states
    private val _playbackUiState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Idle)
    val playbackUiState: StateFlow<PlaybackUiState> = _playbackUiState.asStateFlow()

    // Playback position (ms) to restore after a seamless audio-language switch. Set by
    // switchAudioLanguage() and consumed once by the player when the new stream is prepared.
    private var pendingResumePositionMs: Long = 0L

    fun consumePendingResumePosition(): Long {
        val p = pendingResumePositionMs
        pendingResumePositionMs = 0L
        return p
    }

    // Search screen states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestionItem>>(emptyList())
    val searchSuggestions: StateFlow<List<SearchSuggestionItem>> = _searchSuggestions.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchItem>>(emptyList())
    val searchResults: StateFlow<List<SearchItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _trendingSearches = MutableStateFlow<List<String>>(emptyList())
    val trendingSearches: StateFlow<List<String>> = _trendingSearches.asStateFlow()

    // Full-search bookkeeping.
    // searchJob: lets a newer full search cancel an older in-flight one (no stale overwrites).
    // lastCommittedQuery: the query the user "committed" via performFullSearch. While a query is
    // committed, the debounced suggestion pipeline must NOT repopulate suggestions for it —
    // otherwise the suggestion dropdown would reappear ~300ms later and hide the results grid.
    private var searchJob: Job? = null
    private var lastCommittedQuery: String? = null

    init {
        loadHomeData()
        loadBanner()
        applyMovieFilters("All", "All", "All", "All", "Popularity")
        applyExploreFilters("All", "All", "All")
        applyAnimeFilters("All", "All", "Popularity")
        initSearchDebounce()
        loadTrendingSearches()
    }

    fun loadTrendingSearches() {
        viewModelScope.launch {
            try {
                val searches = repository.getTrendingSearches()
                _trendingSearches.value = searches
            } catch (e: Exception) {
                _trendingSearches.value = listOf("Action", "Comedy", "Sci-Fi", "Love", "Drama", "Fantasy", "Adventure")
            }
        }
    }

    fun loadBanner() {
        viewModelScope.launch {
            try {
                val banners = repository.getBannerItems()
                _bannerItems.value = banners
                android.util.Log.d("HOME_DEBUG", "banner count = ${_bannerItems.value.size}")
            } catch (e: Exception) {
                android.util.Log.e("HOME_API", "Error in loadBanner", e)
            }
        }
    }

    fun loadHomeData() {
        android.util.Log.d("HOME_DEBUG", "loadHomeData called")
        viewModelScope.launch {
            _isHomeLoading.value = true
            try {
                // 1. Fetch remote sections dynamically
                val sections = repository.getHomeSections()
                android.util.Log.d("HOME_DEBUG", "sections count = ${sections.size}")
                _homeSections.value = sections

                // 2. Populate fallback/legacy states so secondary screens do not break
                val bannerSection = sections.find { it.type == "BANNER" }
                val bannerItem = bannerSection?.banner?.items?.firstOrNull()?.subject
                _heroBanner.value = if (bannerItem != null) {
                    MediaSummary(
                        id = bannerItem.subjectId,
                        title = bannerItem.title,
                        posterPath = bannerItem.cover?.url ?: "",
                        backdropPath = bannerItem.stills?.url ?: bannerItem.cover?.url ?: "",
                        mediaType = if (bannerItem.subjectType == 1) "movie" else "tv",
                        rating = bannerItem.imdbRatingValue?.toDoubleOrNull() ?: 0.0,
                        year = bannerItem.releaseDate?.take(4) ?: "",
                        genres = bannerItem.genre?.split(",") ?: emptyList()
                    )
                } else null

                val animeSection = sections.find { it.type == "SUBJECTS_ANIME" || it.title.lowercase().contains("anime") }
                val animeItems = animeSection?.subjects?.map {
                    MediaSummary(
                        id = it.subjectId,
                        title = it.title,
                        posterPath = it.cover?.url ?: "",
                        backdropPath = it.stills?.url ?: it.cover?.url ?: "",
                        mediaType = "anime",
                        rating = it.imdbRatingValue?.toDoubleOrNull() ?: 0.0,
                        year = it.releaseDate?.take(4) ?: "",
                        genres = it.genre?.split(",") ?: emptyList()
                    )
                } ?: emptyList()

                if (animeItems.isNotEmpty()) {
                    _animationFeed.value = animeItems
                } else {
                    _animationFeed.value = repository.getAnimationFeed()
                }

                _trending.value = repository.getTrending()
                _popularMovies.value = repository.getPopularMovies()
                _popularTVShows.value = repository.getPopularTVShows()
                _recentlyAdded.value = repository.getRecentlyAdded()
                _topRated.value = repository.getTopRated()
            } catch (e: Exception) {
                // Silent fail or default states
            } finally {
                _isHomeLoading.value = false
            }
        }
    }



    fun search(query: String) {
        _searchQuery.value = query
        // The user is editing the text again → this is no longer a committed full-search query,
        // so live suggestions should resume for the new text.
        if (query.trim() != lastCommittedQuery) {
            lastCommittedQuery = null
        }
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchSuggestions.value = emptyList()
            _searchError.value = null
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun initSearchDebounce() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    val trimmed = query.trim()
                    // Skip the suggestion fetch entirely for a query that was committed to a full
                    // search — its results are already on screen and must stay visible.
                    if (trimmed == lastCommittedQuery) return@collect

                    if (trimmed.length >= 2) {
                        try {
                            val encodedQuery = java.net.URLEncoder.encode(trimmed, "UTF-8")
                            android.util.Log.d("SEARCH_QUERY", "Fetching suggestions for: $trimmed")
                            android.util.Log.d("SEARCH_REQUEST_URL", "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/search-suggest?query=$encodedQuery")

                            val response = repository.searchSuggest(trimmed)
                            val items = response.data?.items ?: emptyList()

                            android.util.Log.d("SEARCH_SUGGESTIONS_COUNT", "Count: ${items.size}")
                            // Re-check after awaiting: if the query was committed to a full search
                            // (or changed) while this request was in flight, drop these suggestions
                            // so they can't overwrite the visible results.
                            if (trimmed != lastCommittedQuery && _searchQuery.value.trim() == trimmed) {
                                _searchSuggestions.value = items
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SEARCH_ERROR", "Error fetching suggestions", e)
                            _searchSuggestions.value = emptyList()
                        }
                    } else {
                        _searchSuggestions.value = emptyList()
                    }
                }
        }
    }

    fun performFullSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        // Mark this query as committed BEFORE touching _searchQuery so the debounced suggestion
        // pipeline (which observes _searchQuery) won't re-fetch suggestions and hide the results.
        lastCommittedQuery = trimmed
        _searchQuery.value = trimmed
        _searchSuggestions.value = emptyList() // clear suggestions list on enter
        _isSearching.value = true
        _searchError.value = null

        // Cancel any previous in-flight full search so an older response can't overwrite this one.
        // Previous results are left untouched until the new ones arrive (no premature clearing).
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val encodedQuery = java.net.URLEncoder.encode(trimmed, "UTF-8")
                android.util.Log.d("SEARCH_QUERY", "Performing full search for: $trimmed")
                android.util.Log.d("SEARCH_REQUEST_URL", "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/search?query=$encodedQuery")

                val response = repository.search(trimmed)
                val items = response.data?.items ?: emptyList()

                android.util.Log.d("SEARCH_RESULTS_COUNT", "Count: ${items.size}")
                items.forEach { item ->
                    android.util.Log.d("SEARCH_RESULT_TITLE", "Title: ${item.title}")
                }

                _searchResults.value = items
            } catch (e: Exception) {
                android.util.Log.e("SEARCH_ERROR", "Error executing full search", e)
                _searchError.value = e.message ?: "Failed to load results. Please try again."
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun loadMediaDetails(id: String) {
        viewModelScope.launch {
            _detailsUiState.value = DetailsUiState.Loading
            try {
                val details = repository.getDetails(id)
                val recommendations = repository.getRecommendations(id)
                _detailsUiState.value = DetailsUiState.Success(details, recommendations)
            } catch (e: Exception) {
                _detailsUiState.value = DetailsUiState.Error(e.message ?: "Failed to load details.")
            }
        }
    }

    fun loadStaffDetails(id: String) {
        viewModelScope.launch {
            _staffUiState.value = StaffUiState.Loading
            try {
                staffId = id
                staffPage = 1
                staffLoadingMore = false
                val page = repository.getStaffFilmographyPage(id, 1, STAFF_PER_PAGE)
                val related = repository.getStaffRelatedCelebrities(id)
                staffHasMore = page.hasMore
                val profile = page.profile
                    ?: StaffProfileInfo(id, "", "", "", "", page.totalTitles)
                _staffUiState.value = StaffUiState.Success(
                    StaffPageData(
                        profile = profile,
                        filmography = page.items,
                        related = related,
                        totalTitles = page.totalTitles,
                        hasMore = page.hasMore,
                        isLoadingMore = false,
                        currentPage = 1
                    )
                )
            } catch (e: Exception) {
                _staffUiState.value = StaffUiState.Error(e.message ?: "Failed to load staff.")
            }
        }
    }

    fun loadMoreStaffFilmography() {
        val current = _staffUiState.value as? StaffUiState.Success ?: return
        if (!staffHasMore || staffLoadingMore) return
        staffLoadingMore = true
        _staffUiState.value = StaffUiState.Success(current.data.copy(isLoadingMore = true))
        viewModelScope.launch {
            try {
                val next = staffPage + 1
                val page = repository.getStaffFilmographyPage(staffId, next, STAFF_PER_PAGE)
                staffPage = next
                staffHasMore = page.hasMore && page.items.isNotEmpty()
                val merged = current.data.filmography + page.items
                _staffUiState.value = StaffUiState.Success(
                    current.data.copy(
                        filmography = merged,
                        hasMore = staffHasMore,
                        isLoadingMore = false,
                        currentPage = next
                    )
                )
            } catch (e: Exception) {
                staffHasMore = false
                _staffUiState.value = StaffUiState.Success(
                    current.data.copy(isLoadingMore = false, hasMore = false)
                )
            } finally {
                staffLoadingMore = false
            }
        }
    }

    fun loadPlayback(mediaId: String, episodeId: String? = null) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _playbackUiState.value = PlaybackUiState.Loading
            try {
                android.util.Log.d("PLAYBACK_FLOW", "=== PLAYBACK FLOW STARTED ===")
                android.util.Log.d("PLAYBACK_FLOW", "SubjectId: $mediaId")
                android.util.Log.d("PLAYBACK_FLOW", "EpisodeId: $episodeId")
                
                val playback = repository.getPlaybackDetails(mediaId, episodeId)
                
                android.util.Log.d("PLAYBACK_FLOW", "=== PLAYBACK RESOLVED ===")
                android.util.Log.d("PLAYBACK_FLOW", "Stream URL: ${playback.streamUrl}")
                android.util.Log.d("PLAYBACK_FLOW", "Format: ${playback.format}")
                android.util.Log.d("PLAYBACK_FLOW", "Qualities available: ${playback.qualities.size}")
                // FLOW_TRACE LOG 6 — ViewModel hand-off to the Player screen
                android.util.Log.e("FLOW_TRACE", "ViewModel received URL = ${playback.streamUrl}")

                val details = repository.getDetails(mediaId)
                android.util.Log.d("PLAYBACK_FLOW", "Details fetched: ${details.title}")

                val recommendations = try {
                    val recs = repository.getRecommendations(mediaId)
                    android.util.Log.d("PLAYBACK_FLOW", "Recommendations: ${recs.size}")
                    recs
                } catch (e: Exception) {
                    android.util.Log.e("PLAYBACK_FLOW", "Failed to fetch recommendations: ${e.message}")
                    emptyList()
                }

                _playbackUiState.value = PlaybackUiState.Success(playback, details, recommendations)
                android.util.Log.d("PLAYBACK_FLOW", "=== PLAYBACK READY FOR PLAYER ===")
            } catch (e: Exception) {
                android.util.Log.e("PLAYBACK_FLOW", "=== PLAYBACK FAILED ===")
                android.util.Log.e("PLAYBACK_FLOW", "Error: ${e.javaClass.simpleName}")
                android.util.Log.e("PLAYBACK_FLOW", "Message: ${e.message}")
                
                // Determine user-friendly error message
                val userMessage = when {
                    e.message?.contains("CLEARTEXT", ignoreCase = true) == true -> {
                        android.util.Log.e("PLAYBACK_FLOW", "ERROR TYPE: Network Security Policy")
                        android.util.Log.e("PLAYBACK_FLOW", "Android is blocking HTTP (cleartext) traffic")
                        android.util.Log.e("PLAYBACK_FLOW", "Check: network_security_config.xml")
                        "Network security policy blocking HTTP. Check backend URL configuration."
                    }
                    e.message?.contains("Connection", ignoreCase = true) == true -> {
                        android.util.Log.e("PLAYBACK_FLOW", "ERROR TYPE: Connection Error")
                        "Cannot connect to backend server. Is it running?"
                    }
                    e.message?.contains("unreachable", ignoreCase = true) == true -> {
                        android.util.Log.e("PLAYBACK_FLOW", "ERROR TYPE: Server Unreachable")
                        "Playback server unreachable. Check network connection and backend URL."
                    }
                    e.message?.contains("Certificate", ignoreCase = true) == true -> {
                        android.util.Log.e("PLAYBACK_FLOW", "ERROR TYPE: Certificate Error")
                        "SSL certificate validation failed. Check backend HTTPS configuration."
                    }
                    e.message?.contains("timeout", ignoreCase = true) == true -> {
                        android.util.Log.e("PLAYBACK_FLOW", "ERROR TYPE: Timeout")
                        "Backend server is taking too long to respond."
                    }
                    else -> e.message ?: "Failed to load playback stream."
                }
                
                _playbackUiState.value = PlaybackUiState.Error(userMessage)
            }
        }
    }

    /**
     * Seamless audio-language (dub) switch from the player. Fetches the selected dub's detail,
     * maps the currently-watched episode onto the new subject (special rule: movies drop season
     * logic; TV keeps the same season/episode, falling back to the first episode when the new
     * language lacks it), remembers the current playback position, then reuses the untouched
     * [loadPlayback] pipeline to reload the stream for the new subjectId. The player restores the
     * position via [consumePendingResumePosition] once the new stream is prepared.
     */
    fun switchAudioLanguage(newSubjectId: String, desiredEpisodeId: String?, positionMs: Long) {
        viewModelScope.launch {
            _playbackUiState.value = PlaybackUiState.Loading
            val targetEpisodeId = try {
                val newDetails = repository.getDetails(newSubjectId)
                when {
                    newDetails.mediaType.equals("movie", ignoreCase = true) -> null
                    desiredEpisodeId != null && newDetails.episodes.any { it.id == desiredEpisodeId } -> desiredEpisodeId
                    else -> newDetails.episodes.firstOrNull()?.id
                }
            } catch (e: Exception) {
                android.util.Log.e("DUB_SWITCH", "Failed to map episode for $newSubjectId: ${e.message}")
                desiredEpisodeId
            }
            pendingResumePositionMs = positionMs.coerceAtLeast(0L)
            loadPlayback(newSubjectId, targetEpisodeId)
        }
    }

    // Interactive updates
    fun toggleWatchlist(summary: MediaSummary) {
        viewModelScope.launch {
            val isPresent = repository.isInWatchlist(summary.id).first()
            if (isPresent) {
                repository.removeFromWatchlist(summary.id)
            } else {
                repository.addToWatchlist(
                    WatchlistItem(
                        mediaId = summary.id,
                        title = summary.title,
                        posterPath = summary.posterPath,
                        mediaType = summary.mediaType,
                        rating = summary.rating,
                        year = summary.year
                    )
                )
            }
        }
    }

    fun toggleFavorite(summary: MediaSummary) {
        viewModelScope.launch {
            val isPresent = repository.isFavorite(summary.id).first()
            if (isPresent) {
                repository.removeFromFavorites(summary.id)
            } else {
                repository.addToFavorites(
                    FavoriteItem(
                        mediaId = summary.id,
                        title = summary.title,
                        posterPath = summary.posterPath,
                        mediaType = summary.mediaType,
                        rating = summary.rating,
                        year = summary.year
                    )
                )
            }
        }
    }

    fun updateWatchProgress(
        mediaId: String,
        title: String,
        posterPath: String,
        mediaType: String,
        episodeId: String?,
        episodeNumber: Int?,
        episodeTitle: String?,
        lastPositionMs: Long,
        durationMs: Long
    ) {
        viewModelScope.launch {
            repository.saveWatchHistory(
                WatchHistoryItem(
                    mediaId = mediaId,
                    title = title,
                    posterPath = posterPath,
                    mediaType = mediaType,
                    lastEpisodeId = episodeId,
                    lastEpisodeNumber = episodeNumber,
                    lastEpisodeTitle = episodeTitle,
                    lastPositionMs = lastPositionMs,
                    durationMs = durationMs,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
