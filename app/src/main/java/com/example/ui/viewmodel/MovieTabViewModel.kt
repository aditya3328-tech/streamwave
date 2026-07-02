package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WatchHistoryItem
import com.example.data.model.BannerItem
import com.example.data.model.HomeSection
import com.example.data.repository.MovieTabRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MovieTabViewModel(
    private val repository: MovieTabRepository = MovieTabRepository(),
) : ViewModel() {
    private val _isMovieTabLoading = MutableStateFlow(false)
    val isMovieTabLoading: StateFlow<Boolean> = _isMovieTabLoading.asStateFlow()

    private val _movieTabSections = MutableStateFlow<List<HomeSection>>(emptyList())
    val movieTabSections: StateFlow<List<HomeSection>> = _movieTabSections.asStateFlow()

    private val _movieTabBanners = MutableStateFlow<List<BannerItem>>(emptyList())
    val movieTabBanners: StateFlow<List<BannerItem>> = _movieTabBanners.asStateFlow()

    private val _movieTabError = MutableStateFlow<String?>(null)
    val movieTabError: StateFlow<String?> = _movieTabError.asStateFlow()

    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded.asStateFlow()

    val watchHistory: StateFlow<List<WatchHistoryItem>> = (repository.watchHistory ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadMovieTabData(forceRefresh: Boolean = false) {
        if (_hasLoaded.value && !forceRefresh) return

        viewModelScope.launch {
            _isMovieTabLoading.value = true
            _movieTabError.value = null
            try {
                val data = repository.getMovieTabData()
                _movieTabBanners.value = data.banners
                _movieTabSections.value = data.sections
                _hasLoaded.value = true
            } catch (e: Exception) {
                _movieTabError.value = e.message ?: "Failed to load Movies"
                if (!_hasLoaded.value) {
                    _movieTabBanners.value = emptyList()
                    _movieTabSections.value = emptyList()
                }
            } finally {
                _isMovieTabLoading.value = false
            }
        }
    }
}
