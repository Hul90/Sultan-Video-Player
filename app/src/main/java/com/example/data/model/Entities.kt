package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey val uri: String,
    val title: String,
    val durationMs: Long,
    val positionMs: Long,
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val aspectRatioMode: String = "FIT"
)

@Entity(tableName = "favorite_videos")
data class FavoriteVideo(
    @PrimaryKey val uri: String,
    val title: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_videos")
data class VaultVideo(
    @PrimaryKey val uri: String,
    val title: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val resolution: String = "HD",
    val mimeType: String = "video/mp4",
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "network_streams")
data class NetworkStreamItem(
    @PrimaryKey val url: String,
    val title: String,
    val streamType: String = "HLS/M3U8",
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)
