package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoItem
import com.example.ui.theme.DarkNavySurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VideoDetailsDialog(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateStr = if (video.dateAdded > 0) {
        dateFormat.format(Date(video.dateAdded * 1000))
    } else "N/A"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavySurface,
        icon = {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = NeonCyan)
        },
        title = {
            Text(
                text = "Video Details",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Title", video.title)
                DetailRow("Duration", video.durationFormatted)
                DetailRow("Size", video.sizeFormatted)
                DetailRow("Quality", video.resolutionBadge)
                if (video.resolution.isNotEmpty()) {
                    DetailRow("Resolution", video.resolution)
                }
                DetailRow("Format / Mime", video.mimeType)
                DetailRow("Folder", video.folderName)
                DetailRow("Date Added", dateStr)
                DetailRow("Location", video.path.ifEmpty { video.uri.toString() })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Normal)
    }
}
