package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Caption
import com.example.data.model.ResolvedStreamPayload
import com.example.data.model.Stream
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PlayerViewModel - Manages video playback state and operations
 * Handles stream loading, quality selection, caption selection, and playback state
 */
class PlayerViewModel(private val context: Context) : ViewModel() {
    
    private val repository = StreamRepository(context)
    
    // UI State
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Idle)
    val uiState = _uiState.asStateFlow()
    
    // Streams & Selection
    private val _streams = MutableStateFlow<List<Stream>>(emptyList())
    val streams = _streams.asStateFlow()
    
    private val _selectedStream = MutableStateFlow<Stream?>(null)
    val selectedStream = _selectedStream.asStateFlow()
    
    // Captions
    private val _captions = MutableStateFlow<List<Caption>>(emptyList())
    val captions = _captions.asStateFlow()
    
    private val _selectedCaption = MutableStateFlow<Caption?>(null)
    val selectedCaption = _selectedCaption.asStateFlow()
    
    // Playback Position
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()
    
    // Episode Info
    private val _currentEpisode = MutableStateFlow(EpisodeInfo())
    val currentEpisode = _currentEpisode.asStateFlow()
    
    /**
     * Load streams for specific episode
     */
    fun loadStreams(
        subjectId: String,
        season: Int = 0,
        episode: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            _currentEpisode.value = EpisodeInfo(
                subjectId = subjectId,
                season = season,
                episode = episode
            )
            
            try {
                val result = repository.getStreams(
                    subjectId = subjectId,
                    season = season,
                    episode = episode
                )
                
                if (result.success && result.selectedStream != null) {
                    _streams.value = result.streams
                    _selectedStream.value = result.selectedStream
                    _uiState.value = PlayerUiState.Success
                    
                    // Load captions for the content
                    loadCaptions(
                        id = result.selectedStream.id,
                        subjectId = subjectId
                    )
                } else {
                    _uiState.value = PlayerUiState.Error(
                        message = "No streams available. Tried all providers.",
                        attempts = result.attempts.size
                    )
                }
            } catch (e: Exception) {
                _uiState.value = PlayerUiState.Error(
                    message = e.message ?: "Unknown error occurred",
                    attempts = 0
                )
            }
        }
    }
    
    /**
     * Load captions for current stream
     */
    private fun loadCaptions(id: String, subjectId: String) {
        viewModelScope.launch {
            try {
                val captions = repository.getCaptions(
                    id = id,
                    subjectId = subjectId
                )
                _captions.value = captions
                
                // Select first caption by default if available
                if (captions.isNotEmpty()) {
                    _selectedCaption.value = captions[0]
                }
            } catch (e: Exception) {
                // Captions loading failed, continue without them
            }
        }
    }
    
    /**
     * Select different stream quality
     */
    fun selectStream(stream: Stream) {
        _selectedStream.value = stream
    }
    
    /**
     * Select caption/subtitle
     */
    fun selectCaption(caption: Caption?) {
        _selectedCaption.value = caption
    }
    
    /**
     * Update current playback position
     */
    fun updatePosition(positionMs: Long) {
        _currentPosition.value = positionMs
    }
    
    /**
     * Update stream duration
     */
    fun updateDuration(durationMs: Long) {
        _duration.value = durationMs
    }
    
    /**
     * Load next episode
     */
    fun nextEpisode() {
        val current = _currentEpisode.value
        loadStreams(
            subjectId = current.subjectId,
            season = current.season,
            episode = current.episode + 1
        )
    }
    
    /**
     * Load previous episode
     */
    fun previousEpisode() {
        val current = _currentEpisode.value
        if (current.episode > 1) {
            loadStreams(
                subjectId = current.subjectId,
                season = current.season,
                episode = current.episode - 1
            )
        }
    }
    
    /**
     * Clear cache
     */
    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
        }
    }
    
    /**
     * Retry loading streams
     */
    fun retry() {
        val current = _currentEpisode.value
        if (current.subjectId.isNotEmpty()) {
            loadStreams(
                subjectId = current.subjectId,
                season = current.season,
                episode = current.episode
            )
        }
    }
}

/**
 * UI State for player screen
 */
sealed class PlayerUiState {
    object Idle : PlayerUiState()
    object Loading : PlayerUiState()
    object Success : PlayerUiState()
    data class Error(val message: String, val attempts: Int) : PlayerUiState()
}

/**
 * Episode information
 */
data class EpisodeInfo(
    val subjectId: String = "",
    val season: Int = 0,
    val episode: Int = 0
)

/**
 * Factory for PlayerViewModel
 */
class PlayerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            return PlayerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
