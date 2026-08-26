package com.example.data.repository

import android.content.ContentUris
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

    suspend fun deleteVideo(video: VideoItem): Boolean = withContext(Dispatchers.IO) {
        try {
            if (video.isDemo) return@withContext true
            val rows = context.contentResolver.delete(video.uri, null, null)
            if (rows > 0) return@withContext true

            if (video.path.isNotEmpty()) {
                val file = File(video.path)
                if (file.exists()) {
                    return@withContext file.delete()
                }
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
