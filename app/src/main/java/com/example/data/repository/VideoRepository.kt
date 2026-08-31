package com.example.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import com.example.data.model.VideoSortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

import android.app.RecoverableSecurityException
import android.content.IntentSender

sealed class DeleteResult {
    object Success : DeleteResult()
    data class RequiresIntentSender(val intentSender: IntentSender, val video: VideoItem) : DeleteResult()
    data class Error(val message: String) : DeleteResult()
}

sealed class RenameResult {
    object Success : RenameResult()
    data class RequiresIntentSender(val intentSender: IntentSender, val video: VideoItem, val newName: String) : RenameResult()
    data class Error(val message: String) : RenameResult()
}

class VideoRepository(private val context: Context) {

    suspend fun loadVideos(sortBy: VideoSortBy = VideoSortBy.DATE_NEWEST, searchQuery: String = ""): List<VideoItem> =
        withContext(Dispatchers.IO) {
            val videoList = mutableListOf<VideoItem>()

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            )

            val sortOrder = when (sortBy) {
                VideoSortBy.NAME_ASC -> "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
                VideoSortBy.NAME_DESC -> "${MediaStore.Video.Media.DISPLAY_NAME} DESC"
                VideoSortBy.DATE_NEWEST -> "${MediaStore.Video.Media.DATE_ADDED} DESC"
                VideoSortBy.DATE_OLDEST -> "${MediaStore.Video.Media.DATE_ADDED} ASC"
                VideoSortBy.SIZE_LARGEST -> "${MediaStore.Video.Media.SIZE} DESC"
                VideoSortBy.SIZE_SMALLEST -> "${MediaStore.Video.Media.SIZE} ASC"
                VideoSortBy.DURATION_LONGEST -> "${MediaStore.Video.Media.DURATION} DESC"
                VideoSortBy.DURATION_SHORTEST -> "${MediaStore.Video.Media.DURATION} ASC"
            }

            val selection = if (searchQuery.isNotBlank()) {
                "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            } else null

            val selectionArgs = if (searchQuery.isNotBlank()) {
                arrayOf("%$searchQuery%")
            } else null

            try {
                val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val cursor: Cursor? = context.contentResolver.query(
                    queryUri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )

                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val dataCol = it.getColumnIndex(MediaStore.Video.Media.DATA)
                    val durationCol = it.getColumnIndex(MediaStore.Video.Media.DURATION)
                    val sizeCol = it.getColumnIndex(MediaStore.Video.Media.SIZE)
                    val dateAddedCol = it.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                    val mimeCol = it.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                    val widthCol = it.getColumnIndex(MediaStore.Video.Media.WIDTH)
                    val heightCol = it.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                    while (it.moveToNext()) {
                        val id = it.getLong(idCol)
                        val name = it.getString(nameCol) ?: "Video_$id"
                        val path = if (dataCol != -1) it.getString(dataCol) ?: "" else ""
                        val duration = if (durationCol != -1) it.getLong(durationCol) else 0L
                        val size = if (sizeCol != -1) it.getLong(sizeCol) else 0L
                        val dateAdded = if (dateAddedCol != -1) it.getLong(dateAddedCol) else 0L
                        val mime = if (mimeCol != -1) it.getString(mimeCol) ?: "video/mp4" else "video/mp4"
                        val width = if (widthCol != -1) it.getInt(widthCol) else 0
                        val height = if (heightCol != -1) it.getInt(heightCol) else 0

                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                        val folderName = if (path.isNotEmpty()) {
                            val parent = File(path).parentFile
                            parent?.name ?: "Storage"
                        } else {
                            "Internal"
                        }

                        videoList.add(
                            VideoItem(
                                id = id,
                                uri = contentUri,
                                title = name,
                                path = path,
                                durationMs = duration,
                                sizeBytes = size,
                                dateAdded = dateAdded,
                                width = width,
                                height = height,
                                resolution = if (width > 0 && height > 0) "${width}x${height}" else "",
                                mimeType = mime,
                                folderName = folderName,
                                isDemo = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return@withContext sortList(videoList, sortBy)
        }

    private fun sortList(list: List<VideoItem>, sortBy: VideoSortBy): List<VideoItem> {
        return when (sortBy) {
            VideoSortBy.NAME_ASC -> list.sortedBy { it.title.lowercase() }
            VideoSortBy.NAME_DESC -> list.sortedByDescending { it.title.lowercase() }
            VideoSortBy.DATE_NEWEST -> list.sortedByDescending { it.dateAdded }
            VideoSortBy.DATE_OLDEST -> list.sortedBy { it.dateAdded }
            VideoSortBy.SIZE_LARGEST -> list.sortedByDescending { it.sizeBytes }
            VideoSortBy.SIZE_SMALLEST -> list.sortedBy { it.sizeBytes }
            VideoSortBy.DURATION_LONGEST -> list.sortedByDescending { it.durationMs }
            VideoSortBy.DURATION_SHORTEST -> list.sortedBy { it.durationMs }
        }
    }

    suspend fun getFolders(sortBy: VideoSortBy = VideoSortBy.NAME_ASC): List<VideoFolder> =
        withContext(Dispatchers.IO) {
            val allVideos = loadVideos(sortBy = sortBy)
            return@withContext getFoldersFromVideos(allVideos)
        }

    fun getFoldersFromVideos(allVideos: List<VideoItem>): List<VideoFolder> {
        val folderMap = mutableMapOf<String, MutableList<VideoItem>>()

        for (video in allVideos) {
            val folderName = video.folderName
            val list = folderMap.getOrPut(folderName) { mutableListOf() }
            list.add(video)
        }

        val folderList = folderMap.map { (name, videos) ->
            val totalSize = videos.sumOf { it.sizeBytes }
            val firstPath = videos.firstOrNull()?.path ?: ""
            val folderPath = if (firstPath.isNotEmpty() && !firstPath.startsWith("http")) {
                File(firstPath).parent ?: name
            } else {
                "folder_$name"
            }
            VideoFolder(
                name = name,
                path = folderPath,
                videoCount = videos.size,
                totalSizeBytes = totalSize,
                latestVideo = videos.maxByOrNull { it.dateAdded }
            )
        }

        return folderList.sortedBy { it.name.lowercase() }
    }

    suspend fun deleteVideo(video: VideoItem): DeleteResult = withContext(Dispatchers.IO) {
        try {
            if (video.isDemo) return@withContext DeleteResult.Success

            if (video.path.isNotEmpty()) {
                val file = File(video.path)
                if (file.exists() && file.delete()) {
                    try {
                        context.contentResolver.delete(video.uri, null, null)
                    } catch (_: Exception) {}
                    return@withContext DeleteResult.Success
                }
            }

            try {
                val rows = context.contentResolver.delete(video.uri, null, null)
                if (rows > 0) return@withContext DeleteResult.Success
            } catch (securityEx: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(video.uri))
                    return@withContext DeleteResult.RequiresIntentSender(pendingIntent.intentSender, video)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityEx is RecoverableSecurityException) {
                    return@withContext DeleteResult.RequiresIntentSender(securityEx.userAction.actionIntent.intentSender, video)
                }
                throw securityEx
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, listOf(video.uri))
                return@withContext DeleteResult.RequiresIntentSender(pendingIntent.intentSender, video)
            }

            return@withContext DeleteResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext DeleteResult.Error(e.localizedMessage ?: "Delete failed")
        }
    }

    suspend fun renameVideo(video: VideoItem, newName: String): RenameResult = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return@withContext RenameResult.Error("Name cannot be empty")

        val extension = when {
            video.title.contains(".") -> "." + video.title.substringAfterLast(".")
            video.path.contains(".") -> "." + video.path.substringAfterLast(".")
            else -> ".mp4"
        }

        val finalFullName = when {
            trimmed.endsWith(extension, ignoreCase = true) -> trimmed
            trimmed.contains(".") -> trimmed
            else -> "$trimmed$extension"
        }

        try {
            if (video.isDemo) return@withContext RenameResult.Success

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, finalFullName)
                put(MediaStore.Video.Media.TITLE, finalFullName.substringBeforeLast("."))
            }

            try {
                val rows = context.contentResolver.update(video.uri, values, null, null)
                if (rows > 0) {
                    if (video.path.isNotEmpty()) {
                        val file = File(video.path)
                        if (file.exists()) {
                            val newFile = File(file.parentFile, finalFullName)
                            file.renameTo(newFile)
                        }
                    }
                    return@withContext RenameResult.Success
                }
            } catch (securityEx: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(video.uri))
                    return@withContext RenameResult.RequiresIntentSender(pendingIntent.intentSender, video, finalFullName)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityEx is RecoverableSecurityException) {
                    return@withContext RenameResult.RequiresIntentSender(securityEx.userAction.actionIntent.intentSender, video, finalFullName)
                }
                throw securityEx
            }

            if (video.path.isNotEmpty()) {
                val file = File(video.path)
                if (file.exists()) {
                    val newFile = File(file.parentFile, finalFullName)
                    if (file.renameTo(newFile)) {
                        return@withContext RenameResult.Success
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createWriteRequest(context.contentResolver, listOf(video.uri))
                return@withContext RenameResult.RequiresIntentSender(pendingIntent.intentSender, video, finalFullName)
            }

            return@withContext RenameResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext RenameResult.Error(e.localizedMessage ?: "Rename failed")
        }
    }
}
