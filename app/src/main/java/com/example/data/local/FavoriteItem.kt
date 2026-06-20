package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteItem(
    @PrimaryKey val mediaId: String,
    val title: String,
    val posterPath: String,
    val mediaType: String, // "movie", "tv", "anime"
    val rating: Double,
    val year: String,
    val addedAt: Long = System.currentTimeMillis()
)
