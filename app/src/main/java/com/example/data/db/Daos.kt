package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FavoriteVideo
import com.example.data.model.NetworkStreamItem
import com.example.data.model.PlaybackHistory
import com.example.data.model.VaultVideo
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTimestamp DESC")
    fun getAllHistory(): Flow<List<PlaybackHistory>>

    @Query("SELECT * FROM playback_history WHERE uri = :uri LIMIT 1")
    suspend fun getHistoryByUri(uri: String): PlaybackHistory?

    @Query("SELECT * FROM playback_history WHERE uri = :uri LIMIT 1")
    fun observeHistoryByUri(uri: String): Flow<PlaybackHistory?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: PlaybackHistory)

    @Query("DELETE FROM playback_history WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearAllHistory()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorite_videos ORDER BY addedTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteVideo>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_videos WHERE uri = :uri)")
    fun isFavorite(uri: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteVideo)

    @Query("DELETE FROM favorite_videos WHERE uri = :uri")
    suspend fun removeFavorite(uri: String)
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_videos ORDER BY addedTimestamp DESC")
    fun getAllVaultVideos(): Flow<List<VaultVideo>>

    @Query("SELECT EXISTS(SELECT 1 FROM vault_videos WHERE uri = :uri)")
    suspend fun isVideoInVault(uri: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToVault(video: VaultVideo)

    @Query("DELETE FROM vault_videos WHERE uri = :uri")
    suspend fun removeFromVault(uri: String)

    @Query("DELETE FROM vault_videos")
    suspend fun clearVault()
}

@Dao
interface StreamHistoryDao {
    @Query("SELECT * FROM network_streams ORDER BY lastPlayedTimestamp DESC")
    fun getAllStreams(): Flow<List<NetworkStreamItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStream(stream: NetworkStreamItem)

    @Query("DELETE FROM network_streams WHERE url = :url")
    suspend fun deleteStream(url: String)

    @Query("DELETE FROM network_streams")
    suspend fun clearStreams()
}
