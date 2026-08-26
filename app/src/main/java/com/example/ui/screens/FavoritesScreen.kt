package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun FavoritesScreen(
    favoritesList: List<FavoriteVideo>,
    allVideos: List<VideoItem>,
    historyList: List<PlaybackHistory>,
    onVideoClicked: (VideoItem, List<VideoItem>) -> Unit,
    onToggleFavorite: (VideoItem, Boolean) -> Unit,
    onShowDetails: (VideoItem) -> Unit,
    onShare: (VideoItem) -> Unit,
    onDeleteVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteUris = favoritesList.map { it.uri }.toSet()
    val favVideos = allVideos.filter { favoriteUris.contains(it.uri.toString()) }
    val historyMap = historyList.associateBy { it.uri }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        if (favVideos.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = HighDensityAccent,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Favorites Yet",
                    color = HighDensityTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mark videos as favorite to quickly access them in your personal playlist.",
                    color = HighDensityTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(favVideos, key = { "${it.id}_${it.uri}" }) { video ->
                    val history = historyMap[video.uri.toString()]
                    VideoCard(
                        video = video,
                        history = history,
                        isFavorite = true,
                        onClick = { onVideoClicked(video, favVideos) },
                        onToggleFavorite = { onToggleFavorite(video, true) },
                        onShowDetails = { onShowDetails(video) },
                        onShare = { onShare(video) },
                        onDelete = { onDeleteVideo(video) }
                    )
                }
            }
        }
    }
}
