package com.example.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long = System.currentTimeMillis() / 1000,
    val width: Int = 0,
    val height: Int = 0,
    val resolution: String = "",
    val mimeType: String = "video/*",
    val folderName: String = "Internal",
    val isDemo: Boolean = false,
    val subtitleUrl: String? = null,
    val thumbnailUrl: String? = null
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val sizeFormatted: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }

    val resolutionBadge: String
        get() {
            return when {
                width >= 3840 || height >= 2160 -> "4K UHD"
                width >= 2560 || height >= 1440 -> "2K QHD"
                width >= 1920 || height >= 1080 -> "1080p FHD"
                width >= 1280 || height >= 720 -> "720p HD"
                resolution.isNotEmpty() -> resolution
                else -> "HD"
            }
        }
}

data class VideoFolder(
    val name: String,
    val path: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val latestVideo: VideoItem? = null
) {
    val totalSizeFormatted: String
        get() {
            val kb = totalSizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}

enum class VideoSortBy(val label: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    DATE_NEWEST("Date (Newest)"),
    DATE_OLDEST("Date (Oldest)"),
    SIZE_LARGEST("Size (Largest)"),
    SIZE_SMALLEST("Size (Smallest)"),
    DURATION_LONGEST("Duration (Longest)"),
    DURATION_SHORTEST("Duration (Shortest)")
}

typealias SortOption = VideoSortBy

enum class AspectRatioMode(val label: String) {
    FIT("Fit to Screen"),
    FILL("Fill / Zoom"),
    STRETCH("Stretch"),
    SIXTEEN_NINE("16:9 Wide"),
    FOUR_THREE("4:3 Standard"),
    ORIGINAL("Original")
}
