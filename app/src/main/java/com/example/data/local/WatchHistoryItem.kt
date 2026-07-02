package com.example.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "watch_history")
data class WatchHistoryItem(
    @PrimaryKey val mediaId: String,
    val title: String,
    val posterPath: String,
    val mediaType: String, // "movie", "tv", "anime"
    val lastEpisodeId: String?,
    val lastEpisodeNumber: Int?,
    val lastEpisodeTitle: String?,
    val lastPositionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
