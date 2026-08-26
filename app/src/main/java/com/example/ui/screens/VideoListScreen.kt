package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FavoriteVideo
import com.example.data.model.PlaybackHistory
import com.example.data.model.VideoItem
import com.example.ui.components.VideoCard
import com.example.ui.theme.HighDensityAccent
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary

@Composable
fun VideoListScreen(
    videos: List<VideoItem>,
    historyList: List<PlaybackHistory>,
    favoritesList: List<FavoriteVideo>,
    isLoading: Boolean,
    onVideoClicked: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem, Boolean) -> Unit,
    onShowDetails: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyMap = historyList.associateBy { it.uri }
    val favoriteUris = favoritesList.map { it.uri }.toSet()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = HighDensityAccent,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (videos.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = HighDensityAccent,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Local Videos Found",
                    color = HighDensityTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant storage permission or transfer videos (MP4, MKV, WebM) to your device storage.",
                    color = HighDensityTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Refresh Media", color = HighDensityBg, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(videos, key = { "${it.id}_${it.uri}" }) { video ->
                    val history = historyMap[video.uri.toString()]
                    val isFavorite = favoriteUris.contains(video.uri.toString())

                    VideoCard(
                        video = video,
                        history = history,
                        isFavorite = isFavorite,
                        onClick = { onVideoClicked(video, videos) },
                        onToggleFavorite = { onToggleFavorite(video, isFavorite) },
                        onShowDetails = { onShowDetails(video) },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = video.mimeType
                                putExtra(Intent.EXTRA_STREAM, video.uri)
                                putExtra(Intent.EXTRA_TEXT, "Watch '${video.title}' with Sultan Video Player")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
                        },
                        onDelete = { onDeleteVideo(video) }
                    )
                }
            }
        }
    }
}
