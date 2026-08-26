package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FavoriteVideo
import com.example.data.model.PlaybackHistory
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import com.example.ui.components.VideoCard
import com.example.ui.theme.HighDensityAccent
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityTextPrimary

@Composable
fun FolderVideosScreen(
    folder: VideoFolder,
    allVideos: List<VideoItem>,
    historyList: List<PlaybackHistory>,
    favoritesList: List<FavoriteVideo>,
    onBack: () -> Unit,
    onVideoClicked: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem, Boolean) -> Unit,
    onShowDetails: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val folderVideos = allVideos.filter { it.folderName == folder.name }
    val historyMap = historyList.associateBy { it.uri }
    val favoriteUris = favoritesList.map { it.uri }.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = HighDensityTextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    color = HighDensityTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${folderVideos.size} videos • ${folder.totalSizeFormatted}",
                    color = HighDensityAccent,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(folderVideos, key = { "${it.id}_${it.uri}" }) { video ->
                val history = historyMap[video.uri.toString()]
                val isFavorite = favoriteUris.contains(video.uri.toString())

                VideoCard(
                    video = video,
                    history = history,
                    isFavorite = isFavorite,
                    onClick = { onVideoClicked(video, folderVideos) },
                    onToggleFavorite = { onToggleFavorite(video, isFavorite) },
                    onShowDetails = { onShowDetails(video) },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = video.mimeType
                            putExtra(Intent.EXTRA_STREAM, video.uri)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                    },
                    onDelete = { onDeleteVideo(video) }
                )
            }
        }
    }
}
