package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    // Watchlist
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getWatchlist(): Flow<List<WatchlistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWatchlist(item: WatchlistItem)

    @Query("DELETE FROM watchlist WHERE mediaId = :mediaId")
    suspend fun removeFromWatchlist(mediaId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE mediaId = :mediaId LIMIT 1)")
    fun isInWatchlist(mediaId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE mediaId = :mediaId LIMIT 1)")
    suspend fun isInWatchlistSync(mediaId: String): Boolean

    // Favorites
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToFavorites(item: FavoriteItem)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun removeFromFavorites(mediaId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId LIMIT 1)")
    fun isFavorite(mediaId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId LIMIT 1)")
    suspend fun isFavoriteSync(mediaId: String): Boolean

    // History
    @Query("SELECT * FROM watch_history ORDER BY updatedAt DESC")
    fun getWatchHistory(): Flow<List<WatchHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWatchHistory(item: WatchHistoryItem)

    @Query("DELETE FROM watch_history WHERE mediaId = :mediaId")
    suspend fun deleteWatchHistory(mediaId: String)

    @Query("SELECT * FROM watch_history WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getWatchHistoryItem(mediaId: String): WatchHistoryItem?
}
