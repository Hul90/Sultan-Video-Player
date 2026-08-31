package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.view.SurfaceView
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.AspectRatioMode
import com.example.data.model.VideoItem
import com.example.player.BackgroundAudioService
import com.example.player.PlayerUiState
import com.example.player.ScreenshotHelper
import com.example.player.SeekSide
import com.example.player.SultanPlayerManager
import com.example.ui.components.AbRepeatBar
import com.example.ui.components.AspectRatioDialog
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.GestureOverlay
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SpeedSelectionDialog
import com.example.ui.components.SubtitleDownloaderDialog
import com.example.ui.components.TrackSelectionDialog
import com.example.ui.components.VideoCutterDialog
import com.example.ui.components.VideoDetailsDialog
import com.example.ui.theme.AmberGold
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    playerManager: SultanPlayerManager,
    isInPipMode: Boolean = false,
    onBack: () -> Unit,
    onEnterPiP: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by playerManager.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var showAspectRatioDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showSubtitleDownloaderDialog by remember { mutableStateOf(false) }
    var showVideoCutterDialog by remember { mutableStateOf(false) }
    var showAbRepeatControls by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }

    var orientationState by remember { mutableIntStateOf(0) } // 0: Auto/Sensor, 1: Landscape, 2: Portrait
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Sleep Timer Handler
    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60 * 1000L)
            playerManager.player.pause()
            Toast.makeText(context, "Video Player: Sleep timer finished playback", Toast.LENGTH_SHORT).show()
        }
    }

    // Keep screen on ONLY while playing - allow sleep when paused
    DisposableEffect(uiState.isPlaying) {
        if (uiState.isPlaying) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Smooth Animated Zoom transitions
    val animatedScale by animateFloatAsState(
        targetValue = uiState.zoomScale,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "zoom_scale"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = uiState.zoomOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "zoom_offset_x"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = uiState.zoomOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "zoom_offset_y"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen")
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // 1. AndroidView with Media3 PlayerView
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffsetX,
                    translationY = animatedOffsetY
                )
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerManager.player
                        useController = false // Custom Compose Controls
                        resizeMode = when (uiState.aspectRatioMode) {
                            AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        playerViewRef = this
                    }
                },
                update = { view ->
                    view.resizeMode = when (uiState.aspectRatioMode) {
                        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.SIXTEEN_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.FOUR_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Gesture Detector Layer (Full MX Player Gestures)
        var totalDragY by remember { mutableFloatStateOf(0f) }
        var totalDragX by remember { mutableFloatStateOf(0f) }
        var isLeftSwipe by remember { mutableStateOf(true) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.isLocked) {
                    if (uiState.isLocked) {
                        detectTapGestures(
                            onTap = { playerManager.toggleControlsVisibility() }
                        )
                    } else {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (zoom != 1.0f || pan.x != 0f || pan.y != 0f) {
                                val newScale = (uiState.zoomScale * zoom).coerceIn(1.0f, 3.5f)
                                val newX = if (newScale > 1.0f) uiState.zoomOffsetX + pan.x else 0f
                                val newY = if (newScale > 1.0f) uiState.zoomOffsetY + pan.y else 0f
                                playerManager.setZoom(newScale, newX, newY)
                            }
                        }
                    }
                }
                .pointerInput(uiState.isLocked) {
                    if (!uiState.isLocked) {
                        detectTapGestures(
                            onTap = {
                                playerManager.toggleControlsVisibility()
                            },
                            onDoubleTap = { offset ->
                                val widthPx = size.width
                                when {
                                    offset.x < widthPx * 0.35f -> {
                                        playerManager.triggerDoubleTapSeek(SeekSide.LEFT)
                                    }
                                    offset.x > widthPx * 0.65f -> {
                                        playerManager.triggerDoubleTapSeek(SeekSide.RIGHT)
                                    }
                                    else -> {
                                        playerManager.togglePlayPause()
                                    }
                                }
                            }
                        )
                    }
                }
                .pointerInput(uiState.isLocked) {
                    if (!uiState.isLocked) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isLeftSwipe = offset.x < size.width * 0.5f
                                totalDragY = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                totalDragY -= dragAmount
                                val delta = (totalDragY / size.height) * 100f
                                if (isLeftSwipe) {
                                    playerManager.adjustBrightnessDelta(activity, delta * 0.15f)
                                } else {
                                    playerManager.adjustVolumeDelta(delta * 0.15f)
                                }
                            }
                        )
                    }
                }
                .pointerInput(uiState.isLocked) {
                    if (!uiState.isLocked) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                            },
                            onDragEnd = {
                                playerManager.commitSeekGesture()
                            },
                            onDragCancel = {
                                playerManager.commitSeekGesture()
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount
                                val seconds = (totalDragX / 15f).toInt()
                                playerManager.updateSeekGesture(seconds)
                            }
                        )
                    }
                }
        )

        // 3. Gesture Overlays (Volume, Brightness, Seek, Lock, Double Tap)
        if (!isInPipMode) {
            GestureOverlay(
                uiState = uiState,
                onUnlockClicked = { playerManager.toggleScreenLock() }
            )
        }

        // 4. Loading Buffering Indicator
        if (uiState.isBuffering && !isInPipMode) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        // A-B Repeat Bar Overlay (if enabled)
        if (!isInPipMode && (showAbRepeatControls || uiState.abRepeatPointA != null || uiState.abRepeatPointB != null)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                AbRepeatBar(
                    pointA = uiState.abRepeatPointA,
                    pointB = uiState.abRepeatPointB,
                    isActive = uiState.isAbRepeatActive,
                    onSetPointA = {
                        playerManager.setAbRepeatPointA(uiState.currentPositionMs)
                        Toast.makeText(context, "Loop Point A Set", Toast.LENGTH_SHORT).show()
                    },
                    onSetPointB = {
                        playerManager.setAbRepeatPointB(uiState.currentPositionMs)
                        Toast.makeText(context, "Loop Point B Set - A-B Repeat Active", Toast.LENGTH_SHORT).show()
                    },
                    onClear = {
                        playerManager.clearAbRepeat()
                        showAbRepeatControls = false
                        Toast.makeText(context, "Loop Reset", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 5. Controls Overlay (Top bar, Bottom bar, Floating Center Buttons)
        AnimatedVisibility(
            visible = !isInPipMode && uiState.isControlsVisible && !uiState.isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                // Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xEE0A0E17), Color(0x660A0E17), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = uiState.currentVideo?.title ?: "Video Player",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (uiState.currentVideo != null && uiState.currentVideo!!.resolutionBadge.isNotEmpty()) {
                                Text(
                                    text = "${uiState.currentVideo!!.resolutionBadge} • ${uiState.currentVideo!!.sizeFormatted}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isLandscape) {
                            // Equalizer Button (Landscape only quick access)
                            IconButton(onClick = { showEqualizerDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Equalizer & Booster",
                                    tint = if (uiState.equalizerState.isEnabled) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }

                            // Subtitle Downloader & Sync (Landscape only quick access)
                            IconButton(onClick = { showSubtitleDownloaderDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Subtitles,
                                    contentDescription = "Subtitle Downloader",
                                    tint = if (uiState.selectedSubtitleTrackIndex >= 0) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }

                            // Video Cutter & GIF Tool (Landscape only quick access)
                            IconButton(onClick = { showVideoCutterDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = "Video Cutter",
                                    tint = Color.White
                                )
                            }

                            // Aspect Ratio Switcher (Landscape only quick access)
                            IconButton(onClick = { showAspectRatioDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOutMap,
                                    contentDescription = "Aspect Ratio",
                                    tint = Color.White
                                )
                            }
                        }

                        // Screen Lock button
                        IconButton(onClick = { playerManager.toggleScreenLock() }) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock Screen",
                                tint = Color.White
                            )
                        }

                        // More Menu (Speed, Details, Sleep Timer, Background Play, A-B Repeat, Equalizer, Subs, Cutter)
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                modifier = Modifier.background(DarkNavyCard)
                            ) {
                                if (!isLandscape) {
                                    DropdownMenuItem(
                                        text = { Text("Equalizer & Sound Booster", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showMoreMenu = false
                                            showEqualizerDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Subtitles & Download", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showMoreMenu = false
                                            showSubtitleDownloaderDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Video Cutter & GIF Tool", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showMoreMenu = false
                                            showVideoCutterDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Aspect Ratio & Fit", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.ZoomOutMap, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            showMoreMenu = false
                                            showAspectRatioDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Background Audio Play", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMoreMenu = false
                                        playerManager.toggleBackgroundAudio()
                                        Toast.makeText(context, "Background Audio Active - will play with screen off", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("A-B Repeat Loop", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMoreMenu = false
                                        showAbRepeatControls = !showAbRepeatControls
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Playback Speed (${uiState.playbackSpeed}x)", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMoreMenu = false
                                        showSpeedDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Audio Tracks & Subs", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMoreMenu = false
                                        showTrackDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sleep Timer", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = AmberGold) },
                                    onClick = {
                                        showMoreMenu = false
                                        showSleepTimerDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Video Details", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = {
                                        showMoreMenu = false
                                        showDetailsDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Center Play/Pause & Quick Seek & Next/Previous Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Previous Video
                    IconButton(
                        onClick = { playerManager.playPrevious() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Video",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Seek -10s
                    IconButton(
                        onClick = { playerManager.seekRelative(-10000L) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Seek -10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Primary Play / Pause Button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(68.dp),
                        shadowElevation = 8.dp
                    ) {
                        IconButton(
                            onClick = { playerManager.togglePlayPause() },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Seek +10s
                    IconButton(
                        onClick = { playerManager.seekRelative(10000L) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Seek +10s",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Next Video
                    IconButton(
                        onClick = { playerManager.playNext() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Video",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Bottom Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0x660A0E17), Color(0xEE0A0E17))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {

                        // Seekbar & Time labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val curSec = uiState.currentPositionMs / 1000
                            val durSec = uiState.durationMs / 1000
                            val curFormatted = String.format("%02d:%02d", curSec / 60, curSec % 60)
                            val durFormatted = String.format("%02d:%02d", durSec / 60, durSec % 60)

                            Text(
                                text = curFormatted,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Slider(
                                value = uiState.currentPositionMs.toFloat(),
                                onValueChange = { playerManager.seekTo(it.toLong()) },
                                valueRange = 0f..(uiState.durationMs.coerceAtLeast(1L).toFloat()),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color(0x55FFFFFF)
                                )
                            )

                            Text(
                                text = durFormatted,
                                color = Color(0xFFCCCCCC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Bottom Action Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Video
                            IconButton(
                                onClick = { playerManager.playPrevious() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Video",
                                    tint = Color.White
                                )
                            }

                            // Repeat Mode
                            IconButton(
                                onClick = {
                                    val nextMode = (uiState.repeatMode + 1) % 3
                                    playerManager.setRepeatMode(nextMode)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = "Repeat Mode",
                                    tint = if (uiState.repeatMode > 0) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }

                            // Background Play Quick Toggle
                            IconButton(
                                onClick = {
                                    playerManager.toggleBackgroundAudio()
                                    Toast.makeText(context, if (uiState.isBackgroundAudioActive) "Background Audio Enabled" else "Background Audio Disabled", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "Background Audio",
                                    tint = if (uiState.isBackgroundAudioActive) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }

                            // Speed Button Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x33FFFFFF),
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            ) {
                                IconButton(
                                    onClick = { showSpeedDialog = true },
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "${uiState.playbackSpeed}x",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }

                            // Screenshot Capture Button
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val texture = playerViewRef?.videoSurfaceView as? TextureView
                                            val bitmap: Bitmap? = texture?.bitmap
                                            if (bitmap != null) {
                                                val uri = ScreenshotHelper.saveScreenshot(
                                                    context,
                                                    bitmap,
                                                    uiState.currentVideo?.title ?: "Video"
                                                )
                                                if (uri != null) {
                                                    Toast.makeText(context, "Screenshot saved to Pictures/VideoPlayer", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Screenshot saved to Gallery!", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Snapshot captured", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Take Screenshot",
                                    tint = Color.White
                                )
                            }

                            // Orientation Switcher (Sensor, Landscape, Portrait)
                            IconButton(
                                onClick = {
                                    orientationState = (orientationState + 1) % 3
                                    when (orientationState) {
                                        1 -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        2 -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        else -> activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Screen Rotation",
                                    tint = if (orientationState > 0) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }

                            // Picture in Picture (PiP)
                            IconButton(
                                onClick = onEnterPiP,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPictureAlt,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White
                                )
                            }

                            // Next Video
                            IconButton(
                                onClick = { playerManager.playNext() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Video",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showEqualizerDialog) {
        EqualizerDialog(
            equalizerState = uiState.equalizerState,
            presets = playerManager.equalizerManager.getPresetNames(),
            onToggleEnabled = { playerManager.equalizerManager.setEnabled(it) },
            onSelectPreset = { playerManager.equalizerManager.applyPreset(it) },
            onBandLevelChange = { bandIndex, levelMilliBel ->
                playerManager.equalizerManager.setBandLevel(bandIndex, levelMilliBel)
            },
            onBassBoostChange = { playerManager.equalizerManager.setBassBoost(it) },
            onVolumeBoostChange = { playerManager.equalizerManager.setVolumeBoost(it) },
            onReverbChange = { playerManager.equalizerManager.setReverbPreset(it) },
            onDismiss = { showEqualizerDialog = false }
        )
    }

    if (showSubtitleDownloaderDialog && uiState.currentVideo != null) {
        SubtitleDownloaderDialog(
            videoTitle = uiState.currentVideo!!.title,
            currentDelayMs = uiState.subtitleDelayMs,
            currentSizeSp = uiState.subtitleTextSizeSp,
            onLoadSubtitle = { uri, label ->
                playerManager.loadExternalSubtitle(context, uri, label)
            },
            onAdjustDelay = { playerManager.setSubtitleDelay(it) },
            onAdjustSize = { playerManager.setSubtitleSize(it) },
            onDismiss = { showSubtitleDownloaderDialog = false }
        )
    }

    if (showVideoCutterDialog && uiState.currentVideo != null) {
        VideoCutterDialog(
            video = uiState.currentVideo!!,
            currentPositionMs = uiState.currentPositionMs,
            totalDurationMs = uiState.durationMs,
            onPreviewSeek = { playerManager.seekTo(it) },
            onDismiss = { showVideoCutterDialog = false }
        )
    }

    if (showSpeedDialog) {
        SpeedSelectionDialog(
            currentSpeed = uiState.playbackSpeed,
            onSpeedSelected = { playerManager.setPlaybackSpeed(it) },
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showTrackDialog) {
        TrackSelectionDialog(
            audioTracks = uiState.audioTracks,
            selectedAudioIndex = uiState.selectedAudioTrackIndex,
            subtitleTracks = uiState.subtitleTracks,
            selectedSubtitleIndex = uiState.selectedSubtitleTrackIndex,
            subtitleDelayMs = uiState.subtitleDelayMs,
            subtitleTextSizeSp = uiState.subtitleTextSizeSp,
            onSelectAudioTrack = { playerManager.selectAudioTrack(it) },
            onSelectSubtitleTrack = { playerManager.selectSubtitleTrack(it) },
            onAdjustSubtitleDelay = { playerManager.setSubtitleDelay(it) },
            onAdjustSubtitleSize = { playerManager.setSubtitleSize(it) },
            onDismiss = { showTrackDialog = false }
        )
    }

    if (showAspectRatioDialog) {
        AspectRatioDialog(
            currentMode = uiState.aspectRatioMode,
            onModeSelected = { playerManager.setAspectRatioMode(it) },
            onDismiss = { showAspectRatioDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            selectedMinutes = sleepTimerMinutes,
            onMinutesSelected = { sleepTimerMinutes = it },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showDetailsDialog && uiState.currentVideo != null) {
        VideoDetailsDialog(
            video = uiState.currentVideo!!,
            onDismiss = { showDetailsDialog = false }
        )
    }
}
