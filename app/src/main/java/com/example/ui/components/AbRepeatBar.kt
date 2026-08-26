package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.HighDensityBorder

@Composable
fun AbRepeatBar(
    pointA: Long?,
    pointB: Long?,
    isActive: Boolean,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatMs = { ms: Long? ->
        if (ms == null) "--:--" else {
            val sec = (ms / 1000).toInt()
            String.format("%02d:%02d", sec / 60, sec % 60)
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkNavyCard.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, HighDensityBorder),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = "A-B Loop",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Point A Button
            TextButton(
                onClick = onSetPointA,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "A: ${formatMs(pointA)}",
                    color = if (pointA != null) MaterialTheme.colorScheme.primary else Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Point B Button
            TextButton(
                onClick = onSetPointB,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "B: ${formatMs(pointB)}",
                    color = if (pointB != null) MaterialTheme.colorScheme.primary else Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (pointA != null || pointB != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reset Loop",
                        tint = Color(0xFFFFB4AB),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
