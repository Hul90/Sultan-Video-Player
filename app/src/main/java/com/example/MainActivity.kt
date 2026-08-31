package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.model.VideoItem
import com.example.player.SultanPlayerManager
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var playerManager: SultanPlayerManager? = null
    private val isInPlayerScreenState = mutableStateOf(false)
    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure High Performance Coil ImageLoader with Video Frame Extraction & Caching
        try {
            val imageLoader = ImageLoader.Builder(this)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("videoplayer_thumbnails"))
                        .maxSizeBytes(150L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .build()
            Coil.setImageLoader(imageLoader)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            playerManager = SultanPlayerManager(this, mainViewModel.historyRepository)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Handle Intent if opened with a media file
        handleIncomingIntent(intent)

        setContent {
            val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(selectedPalette = mainUiState.selectedPalette) {
                val hasStoragePermission = remember {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_VIDEO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val granted = permissions.values.any { it }
                    if (granted) {
                        hasStoragePermission.value = true
                    }
                    mainViewModel.loadMedia()
                }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                        }
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                        }
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }

                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                val isPlayingScreen by isInPlayerScreenState
                val activeManager = playerManager

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = HighDensityBg
                ) {
                    if (isPlayingScreen && activeManager != null) {
                        BackHandler {
                            try {
                                activeManager.saveHistoryPosition()
                                activeManager.player.pause()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            isInPlayerScreenState.value = false
                        }

                        PlayerScreen(
                            playerManager = activeManager,
                            isInPipMode = isInPipModeState.value,
                            onBack = {
                                try {
                                    activeManager.saveHistoryPosition()
                                    activeManager.player.pause()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                isInPlayerScreenState.value = false
                            },
                            onEnterPiP = {
                                enterPipMode()
                            }
                        )
                    } else {
                        DashboardScreen(
                            viewModel = mainViewModel,
                            hasStoragePermission = hasStoragePermission.value,
                            onRequestPermissions = {
                                val permissionsToRequest = mutableListOf<String>()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                            },
                            onPlayVideo = { video, playlist ->
                                activeManager?.playVideo(video, playlist)
                                isInPlayerScreenState.value = true
                            }
                        )
                    }
                }
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        try {
            val uri: Uri? = intent?.data
            if (uri != null && (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND)) {
                val title = uri.lastPathSegment ?: "External Video"
                val videoItem = VideoItem(
                    id = uri.hashCode().toLong(),
                    uri = uri,
                    title = title,
                    path = uri.path ?: "",
                    durationMs = 0L,
                    sizeBytes = 0L,
                    dateAdded = System.currentTimeMillis() / 1000,
                    resolution = "HD",
                    mimeType = intent.type ?: "video/*",
                    folderName = "External"
                )
                playerManager?.playVideo(videoItem, listOf(videoItem))
                isInPlayerScreenState.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                try {
                    @Suppress("DEPRECATION")
                    enterPictureInPictureMode()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        try {
            if (isInPlayerScreenState.value && playerManager?.player?.isPlaying == true) {
                if (playerManager?.uiState?.value?.isBackgroundAudioActive != true) {
                    enterPipMode()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            playerManager?.setControlsVisibility(false)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isInPipModeState.value) {
            playerManager?.saveHistoryPosition()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager?.release()
        playerManager = null
    }
}
