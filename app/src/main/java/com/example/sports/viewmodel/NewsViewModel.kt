package com.example.sports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports.model.News
import com.example.sports.repository.SportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Backs the News tab (trending list) and the Article detail screen. */
class NewsViewModel(
    private val repository: SportsRepository = SportsRepository.instance,
) : ViewModel() {

    private val _list = MutableStateFlow<SportsUiState<List<News>>>(SportsUiState.Loading)
    val list: StateFlow<SportsUiState<List<News>>> = _list.asStateFlow()

    private val _article = MutableStateFlow<SportsUiState<News>>(SportsUiState.Loading)
    val article: StateFlow<SportsUiState<News>> = _article.asStateFlow()

    init { loadTrending() }

    fun loadTrending() {
        _list.value = SportsUiState.Loading
        viewModelScope.launch {
            _list.value = try {
                SportsUiState.Success(repository.getTrendingNews())
            } catch (e: Exception) {
                SportsUiState.Error(e.message ?: "Failed to load news")
            }
        }
    }

    fun loadArticle(id: String) {
        _article.value = SportsUiState.Loading
        viewModelScope.launch {
            _article.value = try {
                SportsUiState.Success(repository.getArticle(id))
            } catch (e: Exception) {
                SportsUiState.Error(e.message ?: "Failed to load article")
            }
        }
    }
}
