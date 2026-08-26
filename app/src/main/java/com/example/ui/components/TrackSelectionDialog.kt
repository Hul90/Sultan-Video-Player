package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.MediaTrackItem
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TrackSelectionDialog(
    audioTracks: List<MediaTrackItem>,
    selectedAudioIndex: Int,
    subtitleTracks: List<MediaTrackItem>,
    selectedSubtitleIndex: Int,
    subtitleDelayMs: Long,
    subtitleTextSizeSp: Int,
    onSelectAudioTrack: (MediaTrackItem?) -> Unit,
    onSelectSubtitleTrack: (MediaTrackItem?) -> Unit,
    onAdjustSubtitleDelay: (Long) -> Unit,
    onAdjustSubtitleSize: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavySurface,
        title = {
            Text(
                text = "Audio & Subtitles",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkNavySurface,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Audio (${audioTracks.size})", fontSize = 13.sp)
                            }
                        },
                        selectedContentColor = NeonCyan,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Subtitles (${subtitleTracks.size})", fontSize = 13.sp)
                            }
                        },
                        selectedContentColor = NeonCyan,
                        unselectedContentColor = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // Audio Tracks
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = "Default stereo audio track active.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            itemsIndexed(audioTracks) { index, track ->
                                val isSelected = index == selectedAudioIndex
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) DarkNavyCard else Color.Transparent)
                                        .clickable { onSelectAudioTrack(track) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectAudioTrack(track) },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = track.name,
                                            color = if (isSelected) NeonCyan else TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (!track.language.isNullOrEmpty()) {
                                            Text(
                                                text = "Lang: ${track.language}",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Subtitle Tracks & Settings
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        item {
                            // Option: Disable Subtitles
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedSubtitleIndex == -1) DarkNavyCard else Color.Transparent)
                                    .clickable { onSelectSubtitleTrack(null) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSubtitleIndex == -1,
                                    onClick = { onSelectSubtitleTrack(null) },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Off / None",
                                    color = if (selectedSubtitleIndex == -1) NeonCyan else TextPrimary,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        itemsIndexed(subtitleTracks) { index, track ->
                            val isSelected = index == selectedSubtitleIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DarkNavyCard else Color.Transparent)
                                    .clickable { onSelectSubtitleTrack(track) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectSubtitleTrack(track) },
                                    colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = track.name,
                                        color = if (isSelected) NeonCyan else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (!track.language.isNullOrEmpty()) {
                                        Text(
                                            text = "Lang: ${track.language}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Subtitle Sync / Delay Controls
                        item {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Subtitle Sync Offset",
                                color = AmberGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { onAdjustSubtitleDelay(subtitleDelayMs - 500) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("-0.5s", color = TextPrimary, fontSize = 12.sp)
                                }
                                Text(
                                    text = "${if (subtitleDelayMs > 0) "+" else ""}${subtitleDelayMs / 1000.0}s",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { onAdjustSubtitleDelay(subtitleDelayMs + 500) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("+0.5s", color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }

                        // Subtitle Font Size Controls
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Subtitle Font Size: ${subtitleTextSizeSp}sp",
                                color = AmberGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { onAdjustSubtitleSize((subtitleTextSizeSp - 2).coerceAtLeast(12)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Smaller A-", color = TextPrimary, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { onAdjustSubtitleSize((subtitleTextSizeSp + 2).coerceAtMost(32)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkNavyCard),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Larger A+", color = TextPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}
