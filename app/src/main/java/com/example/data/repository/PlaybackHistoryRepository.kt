package com.example.data.repository

import com.example.data.db.FavoriteDao
import com.example.data.db.PlaybackHistoryDao
import com.example.data.model.FavoriteVideo
import com.example.data.model.PlaybackHistory
import kotlinx.coroutines.flow.Flow

class PlaybackHistoryRepository(
    private val historyDao: PlaybackHistoryDao,
    private val favoriteDao: FavoriteDao
) {
    val allHistory: Flow<List<PlaybackHistory>> = historyDao.getAllHistory()
    val allFavorites: Flow<List<FavoriteVideo>> = favoriteDao.getAllFavorites()

    suspend fun getHistory(uri: String): PlaybackHistory? = historyDao.getHistoryByUri(uri)

    fun observeHistory(uri: String): Flow<PlaybackHistory?> = historyDao.observeHistoryByUri(uri)

    suspend fun saveProgress(
        uri: String,
        title: String,
        durationMs: Long,
        positionMs: Long,
        speed: Float = 1.0f,
        aspectRatio: String = "FIT"
    ) {
        val isCompleted = durationMs > 0 && positionMs >= (durationMs * 0.95)
        val history = PlaybackHistory(
            uri = uri,
            title = title,
            durationMs = durationMs,
            positionMs = if (isCompleted) 0 else positionMs,
            lastPlayedTimestamp = System.currentTimeMillis(),
            isCompleted = isCompleted,
            playbackSpeed = speed,
            aspectRatioMode = aspectRatio
        )
        historyDao.insertOrUpdate(history)
    }

    suspend fun deleteHistory(uri: String) = historyDao.deleteByUri(uri)

    suspend fun clearHistory() = historyDao.clearAllHistory()

    fun isFavorite(uri: String): Flow<Boolean> = favoriteDao.isFavorite(uri)

    suspend fun toggleFavorite(uri: String, title: String, currentIsFavorite: Boolean) {
        if (currentIsFavorite) {
            favoriteDao.removeFavorite(uri)
        } else {
            favoriteDao.addFavorite(FavoriteVideo(uri = uri, title = title))
        }
    }
}
