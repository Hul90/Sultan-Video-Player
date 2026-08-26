package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.NetworkStreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class StreamHistoryRepository(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val streamDao = database.streamHistoryDao()

    val streamsFlow: Flow<List<NetworkStreamItem>> = streamDao.getAllStreams()

    suspend fun saveStream(url: String, title: String, streamType: String = "Online Stream") = withContext(Dispatchers.IO) {
        val item = NetworkStreamItem(
            url = url,
            title = if (title.isBlank()) extractTitleFromUrl(url) else title,
            streamType = streamType,
            lastPlayedTimestamp = System.currentTimeMillis()
        )
        streamDao.insertOrUpdateStream(item)
    }

    suspend fun deleteStream(url: String) = withContext(Dispatchers.IO) {
        streamDao.deleteStream(url)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        streamDao.clearStreams()
    }

    private fun extractTitleFromUrl(url: String): String {
        return try {
            val clean = url.substringBefore("?").substringBefore("#")
            val filename = clean.substringAfterLast("/")
            if (filename.isNotBlank()) filename else "Network Stream"
        } catch (e: Exception) {
            "Network Stream"
        }
    }
}
