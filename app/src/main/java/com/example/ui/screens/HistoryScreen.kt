package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlaybackHistory
import com.example.data.model.VideoItem
import com.example.ui.theme.HighDensityAccent
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityCard
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.HighDensityTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyList: List<PlaybackHistory>,
    allVideos: List<VideoItem>,
    onPlayHistoryItem: (PlaybackHistory, VideoItem?) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val videoMap = allVideos.associateBy { it.uri.toString() }
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        if (historyList.isEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = HighDensityAccent,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Watch History",
                    color = HighDensityTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Videos you watch will appear here with automatic resume points.",
                    color = HighDensityTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Clear All button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Played (${historyList.size})",
                        color = HighDensityTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onClearHistory) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList, key = { "history_${it.uri}" }) { history ->
                        val video = videoMap[history.uri]

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayHistoryItem(history, video) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = HighDensityCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0x3300E5FF),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = HighDensityAccent,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = history.title,
                                            color = HighDensityTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        val curSec = history.positionMs / 1000
                                        val durSec = history.durationMs / 1000
                                        val curFormatted = String.format("%02d:%02d", curSec / 60, curSec % 60)
                                        val durFormatted = String.format("%02d:%02d", durSec / 60, durSec % 60)

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (history.isCompleted) "Completed" else "Watched $curFormatted / $durFormatted",
                                                color = if (history.isCompleted) HighDensityAccent else Color(0xFFFFB300),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = " • ",
                                                color = HighDensityTextTertiary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = dateFormat.format(Date(history.lastPlayedTimestamp)),
                                                color = HighDensityTextTertiary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                if (history.durationMs > 0 && !history.isCompleted) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val progress = (history.positionMs.toFloat() / history.durationMs).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = HighDensityAccent,
                                        trackColor = Color(0x33FFFFFF)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
