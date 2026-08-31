package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PlaybackHistory
import com.example.data.model.VideoItem
import com.example.ui.theme.HighDensityAccent
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensityCard
import com.example.ui.theme.HighDensitySubtle
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.HighDensityTextTertiary

@Composable
fun VideoCard(
    video: VideoItem,
    history: PlaybackHistory? = null,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShowDetails: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("video_card_${video.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HighDensityCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Density Thumbnail container
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HighDensitySubtle)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(video.thumbnailUrl ?: video.uri)
                        .crossfade(150)
                        .size(240, 160)
                        .memoryCacheKey("thumb_${video.id}")
                        .diskCacheKey("thumb_${video.id}")
                        .build(),
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Play icon overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x1F000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x99000000),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(3.dp)
                                .size(16.dp)
                        )
                    }
                }

                // Resolution Badge
                if (video.resolutionBadge.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (video.resolutionBadge.contains("4K")) HighDensityAccent else Color(0xCC1A1C1E),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = video.resolutionBadge,
                            color = if (video.resolutionBadge.contains("4K")) Color(0xFF381E72) else HighDensityAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }

                // Duration badge
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = Color(0xDD1A1C1E),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                ) {
                    Text(
                        text = video.durationFormatted,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }

                // Resume progress bar
                if (history != null && history.durationMs > 0 && history.positionMs > 0 && !history.isCompleted) {
                    val progress = (history.positionMs.toFloat() / history.durationMs).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = HighDensityAccent,
                        trackColor = Color(0x55000000)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Metadata Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 1.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = video.title,
                    color = HighDensityTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = video.sizeFormatted,
                        color = HighDensityTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = " • ",
                        color = HighDensityTextTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = video.folderName,
                        color = HighDensityAccent,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (history != null && history.positionMs > 0 && !history.isCompleted) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val resumeSec = history.positionMs / 1000
                    val resumeMin = resumeSec / 60
                    val resumeFormatted = String.format("%02d:%02d", resumeMin, resumeSec % 60)
                    Text(
                        text = "Resume: $resumeFormatted",
                        color = HighDensityAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action & Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("video_menu_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = HighDensityTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(HighDensitySurface)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Remove from Playlist" else "Add to Playlist", color = HighDensityTextPrimary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) HighDensityAccent else HighDensityTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Details & Info", color = HighDensityTextPrimary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = HighDensityTextSecondary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            showMenu = false
                            onShowDetails()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share Video", color = HighDensityTextPrimary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, contentDescription = null, tint = HighDensityTextSecondary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            showMenu = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = HighDensityTextPrimary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = "Rename", tint = HighDensityTextSecondary, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Video", color = Color(0xFFFFB4AB), fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFFFB4AB), modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
