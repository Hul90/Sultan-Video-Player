package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.ActiveGestureType
import com.example.player.PlayerUiState
import com.example.player.SeekSide
import com.example.ui.theme.AmberGold
import com.example.ui.theme.NeonCyan

@Composable
fun GestureOverlay(
    uiState: PlayerUiState,
    onUnlockClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {

        // 1. Screen Lock Indicator & Unlock Button
        if (uiState.isLocked) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(24.dp),
                shape = CircleShape,
                color = Color(0xCC0A0E17),
                shadowElevation = 8.dp
            ) {
                IconButton(
                    onClick = onUnlockClicked,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Screen",
                        tint = AmberGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            return@Box
        }

        // 2. Brightness Overlay (Left Side Swipe)
        AnimatedVisibility(
            visible = uiState.activeGesture == ActiveGestureType.BRIGHTNESS,
            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
            exit = fadeOut(tween(250)) + scaleOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD0A0E17),
                modifier = Modifier.padding(16.dp),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (uiState.brightnessPercent > 50) Icons.Default.BrightnessHigh else Icons.Default.BrightnessLow,
                        contentDescription = "Brightness",
                        tint = AmberGold,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        LinearProgressIndicator(
                            progress = { uiState.brightnessPercent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = AmberGold,
                            trackColor = Color.Transparent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Brightness ${uiState.brightnessPercent}%",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 3. Volume Overlay (Right Side Swipe)
        AnimatedVisibility(
            visible = uiState.activeGesture == ActiveGestureType.VOLUME,
            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
            exit = fadeOut(tween(250)) + scaleOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD0A0E17),
                modifier = Modifier.padding(16.dp),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val volumeIcon = when {
                        uiState.volumePercent == 0 -> Icons.AutoMirrored.Filled.VolumeMute
                        uiState.volumePercent < 50 -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    }
                    Icon(
                        imageVector = volumeIcon,
                        contentDescription = "Volume",
                        tint = NeonCyan,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x33FFFFFF))
                    ) {
                        LinearProgressIndicator(
                            progress = { uiState.volumePercent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = NeonCyan,
                            trackColor = Color.Transparent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Volume ${uiState.volumePercent}%",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 4. Horizontal Seek Gesture Overlay
        AnimatedVisibility(
            visible = uiState.activeGesture == ActiveGestureType.SEEK,
            enter = fadeIn(tween(150)) + scaleIn(tween(150)),
            exit = fadeOut(tween(250)) + scaleOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xEE0A0E17),
                modifier = Modifier.padding(16.dp),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isForward = uiState.seekDeltaSeconds >= 0
                    Icon(
                        imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = "Seek",
                        tint = if (isForward) NeonCyan else AmberGold,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val sign = if (isForward) "+" else ""
                    Text(
                        text = "$sign${uiState.seekDeltaSeconds}s",
                        color = if (isForward) NeonCyan else AmberGold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val targetSec = uiState.seekTargetMs / 1000
                    val durSec = uiState.durationMs / 1000
                    val targetFormatted = String.format("%02d:%02d", targetSec / 60, targetSec % 60)
                    val durFormatted = String.format("%02d:%02d", durSec / 60, durSec % 60)
                    Text(
                        text = "[$targetFormatted / $durFormatted]",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 5. Double Tap Quick Seek Ripple Indicators
        AnimatedVisibility(
            visible = uiState.doubleTapSeekSide == SeekSide.LEFT,
            enter = fadeIn(tween(100)) + scaleIn(tween(100)),
            exit = fadeOut(tween(300)) + scaleOut(tween(300)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xAA000000),
                modifier = Modifier.size(80.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10s",
                        tint = NeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "-10s",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.doubleTapSeekSide == SeekSide.RIGHT,
            enter = fadeIn(tween(100)) + scaleIn(tween(100)),
            exit = fadeOut(tween(300)) + scaleOut(tween(300)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xAA000000),
                modifier = Modifier.size(80.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10s",
                        tint = NeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "+10s",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
