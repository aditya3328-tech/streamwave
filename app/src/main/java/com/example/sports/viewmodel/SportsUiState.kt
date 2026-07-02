package com.example.sports.viewmodel

/** Generic loading container shared by every Sports ViewModel. */
sealed interface SportsUiState<out T> {
    data object Loading : SportsUiState<Nothing>
    data class Success<T>(val data: T) : SportsUiState<T>
    data class Error(val message: String) : SportsUiState<Nothing>
}
