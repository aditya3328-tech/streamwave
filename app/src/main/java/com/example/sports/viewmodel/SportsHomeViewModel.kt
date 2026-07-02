package com.example.sports.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sports.model.LeagueChip
import com.example.sports.model.Match
import com.example.sports.model.SportsSection
import com.example.sports.repository.SportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives both the Sports "Home" tab (titled sections + filter chips) and the "Matches" tab
 * (flat list). Independent of the movie [com.example.ui.viewmodel.CineVerseViewModel].
 */
class SportsHomeViewModel(
    private val repository: SportsRepository = SportsRepository.instance,
) : ViewModel() {

    private val _home = MutableStateFlow<SportsUiState<List<SportsSection>>>(SportsUiState.Loading)
    val home: StateFlow<SportsUiState<List<SportsSection>>> = _home.asStateFlow()

    private val _matches = MutableStateFlow<SportsUiState<List<Match>>>(SportsUiState.Loading)
    val matches: StateFlow<SportsUiState<List<Match>>> = _matches.asStateFlow()

    private val _selectedChip = MutableStateFlow("all")
    val selectedChip: StateFlow<String> = _selectedChip.asStateFlow()

    val chips: List<LeagueChip> = listOf(
        LeagueChip("all", "All"),
        LeagueChip("fifa", "FIFA World Cup"),
        LeagueChip("epl", "Premier League"),
        LeagueChip("more", "More ▼"),
    )

    init {
        loadHome()
        loadMatches()
    }

    fun loadHome() {
        _home.value = SportsUiState.Loading
        viewModelScope.launch {
            _home.value = try {
                SportsUiState.Success(repository.getHomeSections())
            } catch (e: Exception) {
                SportsUiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun loadMatches(tag: String? = null) {
        _matches.value = SportsUiState.Loading
        viewModelScope.launch {
            _matches.value = try {
                SportsUiState.Success(repository.getMatches(tag))
            } catch (e: Exception) {
                SportsUiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun selectChip(id: String) {
        if (id == "more") return
        _selectedChip.value = id
        loadMatches(if (id == "all") null else id)
    }
}
