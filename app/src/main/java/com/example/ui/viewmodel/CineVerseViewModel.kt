package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteItem
import com.example.data.local.WatchHistoryItem
import com.example.data.local.WatchlistItem
import com.example.data.model.*
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    object Loading : DetailsUiState
    data class Success(val details: MediaDetails, val recommendations: List<MediaSummary>) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

sealed interface StaffUiState {
    object Loading : StaffUiState
    data class Success(val staff: StaffDetails) : StaffUiState
    data class Error(val message: String) : StaffUiState
}

sealed interface PlaybackUiState {
    object Idle : PlaybackUiState
    object Loading : PlaybackUiState
    data class Success(val playback: StreamPlayback, val mediaDetails: MediaDetails, val recommendations: List<MediaSummary> = emptyList()) : PlaybackUiState
    data class Error(val message: String) : PlaybackUiState
}

class CineVerseViewModel(private val repository: MovieRepository) : ViewModel() {

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


    // TV Show filter selections
    private val _tvSelectedGenre = MutableStateFlow("All")
    val tvSelectedGenre: StateFlow<String> = _tvSelectedGenre.asStateFlow()

    private val _tvSelectedCountry = MutableStateFlow("All")
    val tvSelectedCountry: StateFlow<String> = _tvSelectedCountry.asStateFlow()

    private val _tvSelectedYear = MutableStateFlow("All")
    val tvSelectedYear: StateFlow<String> = _tvSelectedYear.asStateFlow()

    private val _tvSelectedLanguage = MutableStateFlow("All")
    val tvSelectedLanguage: StateFlow<String> = _tvSelectedLanguage.asStateFlow()

    private val _tvSelectedSortBy = MutableStateFlow("Popularity")
    val tvSelectedSortBy: StateFlow<String> = _tvSelectedSortBy.asStateFlow()

    private val _isTvLoading = MutableStateFlow(false)
    val isTvLoading: StateFlow<Boolean> = _isTvLoading.asStateFlow()

    private val _tvList = MutableStateFlow<List<MediaSummary>>(emptyList())
    val tvList: StateFlow<List<MediaSummary>> = _tvList.asStateFlow()


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
    val tvFilterGenre: StateFlow<String> = _tvSelectedGenre


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

    fun applyTvFilters(genre: String, country: String, year: String, language: String, sortBy: String) {
        _tvSelectedGenre.value = genre
        _tvSelectedCountry.value = country
        _tvSelectedYear.value = year
        _tvSelectedLanguage.value = language
        _tvSelectedSortBy.value = sortBy

        viewModelScope.launch {
            _isTvLoading.value = true
            try {
                _tvList.value = repository.getFilteredSubjects(
                    type = 2,
                    genre = genre,
                    country = country,
                    year = year,
                    language = language,
                    sortBy = sortBy
                )
            } catch (e: Exception) {
                _tvList.value = emptyList()
            } finally {
                _isTvLoading.value = false
            }
        }
    }

    fun resetTvFilters() {
        applyTvFilters("All", "All", "All", "All", "Popularity")
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

    fun setTvFilter(genre: String) {
        applyTvFilters(genre, _tvSelectedCountry.value, _tvSelectedYear.value, _tvSelectedLanguage.value, _tvSelectedSortBy.value)
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

    // Player screen states
    private val _playbackUiState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Idle)
    val playbackUiState: StateFlow<PlaybackUiState> = _playbackUiState.asStateFlow()

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

    init {
        loadHomeData()
        loadBanner()
        applyMovieFilters("All", "All", "All", "All", "Popularity")
        applyTvFilters("All", "All", "All", "All", "Popularity")
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
            } catch (e: Exception) {
                android.util.Log.e("HOME_API", "Error in loadBanner", e)
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isHomeLoading.value = true
            try {
                // 1. Fetch remote sections dynamically
                val sections = repository.getHomeSections()
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
                    if (trimmed.length >= 2) {
                        try {
                            val encodedQuery = java.net.URLEncoder.encode(trimmed, "UTF-8")
                            android.util.Log.d("SEARCH_QUERY", "Fetching suggestions for: $trimmed")
                            android.util.Log.d("SEARCH_REQUEST_URL", "https://h5-api.aoneroom.com/wefeed-h5api-bff/subject/search-suggest?query=$encodedQuery")

                            val response = repository.searchSuggest(trimmed)
                            val items = response.data?.items ?: emptyList()

                            android.util.Log.d("SEARCH_SUGGESTIONS_COUNT", "Count: ${items.size}")
                            _searchSuggestions.value = items
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
        _searchQuery.value = trimmed
        _searchSuggestions.value = emptyList() // clear suggestions list on enter
        _isSearching.value = true
        _searchError.value = null

        viewModelScope.launch {
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
                val staff = repository.getStaffDetails(id)
                _staffUiState.value = StaffUiState.Success(staff)
            } catch (e: Exception) {
                _staffUiState.value = StaffUiState.Error(e.message ?: "Failed to load staff.")
            }
        }
    }

    fun loadPlayback(mediaId: String, episodeId: String? = null) {
        viewModelScope.launch {
            _playbackUiState.value = PlaybackUiState.Loading
            try {
                android.util.Log.d("STREAM_REQUEST", "STREAM_REQUEST subjectId=$mediaId, episodeId=$episodeId")
                val playback = repository.getPlaybackDetails(mediaId, episodeId)
                android.util.Log.d("STREAM_RESPONSE", "STREAM_RESPONSE Stream URL determined: ${playback.streamUrl}")
                android.util.Log.d("SELECTED_STREAM", "SELECTED_STREAM Selected stream: ${playback.streamUrl}")

                val details = repository.getDetails(mediaId)
                android.util.Log.d("DETAIL_API_RESPONSE", "DETAIL_API_RESPONSE Title: ${details.title}, Seasons: ${details.seasons.size}")

                val recommendations = try {
                    val recs = repository.getRecommendations(mediaId)
                    android.util.Log.d("RECOMMENDATION_COUNT", "RECOMMENDATION_COUNT quantity: ${recs.size}")
                    recs
                } catch (e: Exception) {
                    android.util.Log.e("RECOMMENDATION", "Failed to fetch recommendations", e)
                    emptyList()
                }

                _playbackUiState.value = PlaybackUiState.Success(playback, details, recommendations)
            } catch (e: Exception) {
                _playbackUiState.value = PlaybackUiState.Error(e.message ?: "Failed to load playback stream.")
            }
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
