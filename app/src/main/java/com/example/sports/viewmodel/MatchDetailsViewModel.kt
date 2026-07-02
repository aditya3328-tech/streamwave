package com.example.sports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports.model.Match
import com.example.sports.model.News
import com.example.sports.repository.SportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Loads a single fixture (for the details screen and the player) plus a small news rail. */
class MatchDetailsViewModel(
    private val repository: SportsRepository = SportsRepository.instance,
) : ViewModel() {

    private val _match = MutableStateFlow<SportsUiState<Match>>(SportsUiState.Loading)
    val match: StateFlow<SportsUiState<Match>> = _match.asStateFlow()

    private val _news = MutableStateFlow<List<News>>(emptyList())
    val news: StateFlow<List<News>> = _news.asStateFlow()

    fun load(matchId: String) {
        _match.value = SportsUiState.Loading
        viewModelScope.launch {
            _match.value = try {
                SportsUiState.Success(repository.getMatch(matchId))
            } catch (e: Exception) {
                SportsUiState.Error(e.message ?: "Failed to load match")
            }
            _news.value = runCatching { repository.getMatchNews(matchId).take(4) }.getOrDefault(emptyList())
        }
    }
}
