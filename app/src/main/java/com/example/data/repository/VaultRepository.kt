package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.model.VaultVideo
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VaultRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val vaultDao = database.vaultDao()

    val vaultVideosFlow: Flow<List<VideoItem>> = vaultDao.getAllVaultVideos().map { list ->
        list.map { vaultVideo ->
            val uri = try {
                Uri.parse(vaultVideo.uri)
            } catch (e: Exception) {
                Uri.EMPTY
            }
            VideoItem(
                id = vaultVideo.uri.hashCode().toLong(),
                uri = uri,
                title = vaultVideo.title,
                path = vaultVideo.path,
                durationMs = vaultVideo.durationMs,
                sizeBytes = vaultVideo.sizeBytes,
                dateAdded = vaultVideo.addedTimestamp,
                resolution = vaultVideo.resolution,
                mimeType = vaultVideo.mimeType,
                folderName = "Private Vault",
                isDemo = false
            )
        }
    }

    suspend fun addVideoToVault(video: VideoItem) = withContext(Dispatchers.IO) {
        val vaultEntry = VaultVideo(
            uri = video.uri.toString(),
            title = video.title,
            path = video.path,
            durationMs = video.durationMs,
            sizeBytes = video.sizeBytes,
            resolution = video.resolution,
            mimeType = video.mimeType,
            addedTimestamp = System.currentTimeMillis()
        )
        vaultDao.addToVault(vaultEntry)
    }

    suspend fun removeVideoFromVault(uri: String) = withContext(Dispatchers.IO) {
        vaultDao.removeFromVault(uri)
    }

    suspend fun isVideoInVault(uri: String): Boolean = withContext(Dispatchers.IO) {
        vaultDao.isVideoInVault(uri)
    }
}
