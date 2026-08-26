package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VideoItem
import com.example.data.model.VideoSortBy
import com.example.ui.components.VideoDetailsDialog
import com.example.ui.theme.HighDensityAccent
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensityBorder
import com.example.ui.theme.HighDensityCard
import com.example.ui.theme.HighDensityOnAccent
import com.example.ui.theme.HighDensitySubtle
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary
import com.example.ui.theme.HighDensityTextTertiary
import com.example.viewmodel.MainTab
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onPlayVideo: (VideoItem, List<VideoItem>) -> Unit,
    modifier: Modifier = Modifier,
    hasStoragePermission: Boolean = true,
    onRequestPermissions: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val favoritesList by viewModel.favoritesList.collectAsStateWithLifecycle()
    val vaultVideos by viewModel.vaultVideos.collectAsStateWithLifecycle()
    val streamHistory by viewModel.streamHistory.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    // Filter out vault videos from general listing so they stay hidden
    val visibleVideos = remember(uiState.videos, vaultVideos) {
        uiState.videos.filter { video -> vaultVideos.none { it.uri == video.uri } }
    }

    // Last Played item lookup
    val lastPlayedHistory = historyList.firstOrNull { it.positionMs > 0 && !it.isCompleted }
        ?: historyList.firstOrNull()
    val lastPlayedVideo = lastPlayedHistory?.let { hist ->
        visibleVideos.firstOrNull { it.uri.toString() == hist.uri }
    } ?: visibleVideos.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HighDensityBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HighDensitySurface)
                    .statusBarsPadding()
            ) {
                // Status & Top Header
                if (isSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search videos or folders...", color = HighDensityTextSecondary, fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = HighDensityBorder,
                                focusedTextColor = HighDensityTextPrimary,
                                unfocusedTextColor = HighDensityTextPrimary,
                                focusedContainerColor = HighDensitySubtle,
                                unfocusedContainerColor = HighDensitySubtle
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                viewModel.setSearchQuery("")
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Search", tint = HighDensityTextPrimary)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // High Density Brand: Sultan + PRO badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sultan",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(top = 1.dp)
                            ) {
                                Text(
                                    text = "PRO MAX",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Action Icons: Search, Sort, Refresh
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { isSearchActive = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = HighDensityTextPrimary, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = { showSortDialog = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", tint = HighDensityTextPrimary, modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = { viewModel.loadMedia() },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = HighDensityTextPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Top Secondary Tab Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HighDensitySurface)
                ) {
                    val tabs = listOf(
                        MainTab.FOLDERS to "FOLDERS",
                        MainTab.ALL_VIDEOS to "VIDEOS",
                        MainTab.NETWORK_STREAM to "STREAM",
                        MainTab.VAULT to "VAULT",
                        MainTab.HISTORY to "RECENT",
                        MainTab.FAVORITES to "SAVED"
                    )

                    tabs.forEach { (tab, title) ->
                        val isSelected = uiState.currentTab == tab && uiState.selectedFolder == null
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (uiState.selectedFolder != null) {
                                        viewModel.selectFolder(null)
                                    }
                                    viewModel.setTab(tab)
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else HighDensityTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HighDensityBorder.copy(alpha = 0.4f))
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = HighDensitySurface,
                contentColor = HighDensityTextPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == MainTab.FOLDERS,
                    onClick = { viewModel.setTab(MainTab.FOLDERS) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Folders", modifier = Modifier.size(20.dp)) },
                    label = { Text("FOLDERS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = HighDensityBorder,
                        unselectedIconColor = HighDensityTextSecondary,
                        unselectedTextColor = HighDensityTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = uiState.currentTab == MainTab.ALL_VIDEOS,
                    onClick = { viewModel.setTab(MainTab.ALL_VIDEOS) },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Videos", modifier = Modifier.size(20.dp)) },
                    label = { Text("VIDEOS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = HighDensityBorder,
                        unselectedIconColor = HighDensityTextSecondary,
                        unselectedTextColor = HighDensityTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = uiState.currentTab == MainTab.NETWORK_STREAM,
                    onClick = { viewModel.setTab(MainTab.NETWORK_STREAM) },
                    icon = { Icon(Icons.Default.Language, contentDescription = "Stream", modifier = Modifier.size(20.dp)) },
                    label = { Text("STREAM", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = HighDensityBorder,
                        unselectedIconColor = HighDensityTextSecondary,
                        unselectedTextColor = HighDensityTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = uiState.currentTab == MainTab.VAULT,
                    onClick = { viewModel.setTab(MainTab.VAULT) },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Vault", modifier = Modifier.size(20.dp)) },
                    label = { Text("VAULT", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = HighDensityBorder,
                        unselectedIconColor = HighDensityTextSecondary,
                        unselectedTextColor = HighDensityTextSecondary
                    )
                )

                NavigationBarItem(
                    selected = uiState.currentTab == MainTab.SETTINGS,
                    onClick = { viewModel.setTab(MainTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp)) },
                    label = { Text("SETTINGS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = HighDensityBorder,
                        unselectedIconColor = HighDensityTextSecondary,
                        unselectedTextColor = HighDensityTextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(HighDensityBg)
        ) {
            // Permission Rationale Notice Card (if storage permission not yet granted)
            if (!hasStoragePermission && uiState.currentTab != MainTab.SETTINGS && uiState.currentTab != MainTab.NETWORK_STREAM) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = HighDensityCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Storage Permission",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Allow storage access to view and play your device videos.",
                                color = HighDensityTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestPermissions,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Hero "Last Played" Card (Shown on main dashboard when videos exist)
            if (uiState.selectedFolder == null && uiState.currentTab != MainTab.SETTINGS && uiState.currentTab != MainTab.VAULT && uiState.currentTab != MainTab.NETWORK_STREAM && lastPlayedVideo != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .clickable {
                            onPlayVideo(lastPlayedVideo, visibleVideos)
                        }
                        .testTag("hero_last_played_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HighDensityCard),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CONTINUE PLAYING",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastPlayedVideo.title,
                                color = HighDensityTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val posFormatted = if (lastPlayedHistory != null && lastPlayedHistory.positionMs > 0) {
                                val curSec = lastPlayedHistory.positionMs / 1000
                                val curMin = curSec / 60
                                String.format("%02d:%02d", curMin, curSec % 60)
                            } else "00:00"
                            Text(
                                text = "$posFormatted / ${lastPlayedVideo.durationFormatted}",
                                color = HighDensityTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play Last Played",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.selectedFolder != null -> {
                        FolderVideosScreen(
                            folder = uiState.selectedFolder!!,
                            allVideos = visibleVideos,
                            historyList = historyList,
                            favoritesList = favoritesList,
                            onBack = { viewModel.selectFolder(null) },
                            onVideoClicked = { video, list -> onPlayVideo(video, list) },
                            onToggleFavorite = { video, fav -> viewModel.toggleFavorite(video, fav) },
                            onShowDetails = { video -> viewModel.showVideoDetails(video) },
                            onDeleteVideo = { video -> viewModel.deleteVideo(video) }
                        )
                    }
                    uiState.currentTab == MainTab.FOLDERS -> {
                        FolderListScreen(
                            folders = uiState.folders,
                            isLoading = uiState.isLoading,
                            onFolderClicked = { folder -> viewModel.selectFolder(folder) },
                            onRefresh = { viewModel.loadMedia() }
                        )
                    }
                    uiState.currentTab == MainTab.ALL_VIDEOS -> {
                        VideoListScreen(
                            videos = visibleVideos,
                            historyList = historyList,
                            favoritesList = favoritesList,
                            isLoading = uiState.isLoading,
                            onVideoClicked = { video, list -> onPlayVideo(video, list) },
                            onToggleFavorite = { video, fav -> viewModel.toggleFavorite(video, fav) },
                            onShowDetails = { video -> viewModel.showVideoDetails(video) },
                            onDeleteVideo = { video -> viewModel.deleteVideo(video) },
                            onRefresh = { viewModel.loadMedia() }
                        )
                    }
                    uiState.currentTab == MainTab.NETWORK_STREAM -> {
                        NetworkStreamScreen(
                            streamHistory = streamHistory,
                            onPlayStream = { videoItem -> onPlayVideo(videoItem, listOf(videoItem)) },
                            onSaveStream = { url, title -> viewModel.saveNetworkStream(url, title) },
                            onDeleteStream = { url -> viewModel.deleteNetworkStream(url) }
                        )
                    }
                    uiState.currentTab == MainTab.VAULT -> {
                        PrivateVaultScreen(
                            vaultVideos = vaultVideos,
                            allAvailableVideos = uiState.videos,
                            onPlayVideo = { videoItem -> onPlayVideo(videoItem, vaultVideos) },
                            onAddVideoToVault = { video -> viewModel.addVideoToVault(video) },
                            onRemoveVideoFromVault = { video -> viewModel.removeVideoFromVault(video) },
                            onBack = { viewModel.setTab(MainTab.FOLDERS) }
                        )
                    }
                    uiState.currentTab == MainTab.HISTORY -> {
                        HistoryScreen(
                            historyList = historyList,
                            allVideos = visibleVideos,
                            onPlayHistoryItem = { hist, video ->
                                val targetVideo = video ?: visibleVideos.firstOrNull { it.uri.toString() == hist.uri }
                                if (targetVideo != null) {
                                    onPlayVideo(targetVideo, visibleVideos)
                                }
                            },
                            onClearHistory = { viewModel.clearHistory() }
                        )
                    }
                    uiState.currentTab == MainTab.FAVORITES -> {
                        FavoritesScreen(
                            favoritesList = favoritesList,
                            allVideos = visibleVideos,
                            historyList = historyList,
                            onVideoClicked = { video, list -> onPlayVideo(video, list) },
                            onToggleFavorite = { video, fav -> viewModel.toggleFavorite(video, fav) },
                            onShowDetails = { video -> viewModel.showVideoDetails(video) },
                            onShare = {},
                            onDeleteVideo = { video -> viewModel.deleteVideo(video) }
                        )
                    }
                    uiState.currentTab == MainTab.SETTINGS -> {
                        SettingsScreen(
                            selectedPalette = uiState.selectedPalette,
                            onSelectPalette = { viewModel.setThemePalette(it) },
                            onOpenVault = { viewModel.setTab(MainTab.VAULT) },
                            onOpenStream = { viewModel.setTab(MainTab.NETWORK_STREAM) }
                        )
                    }
                }
            }
        }
    }

    // Sort Selection Dialog
    if (showSortDialog) {
        val sortOptions = listOf(
            VideoSortBy.DATE_NEWEST to "Date (Newest first)",
            VideoSortBy.DATE_OLDEST to "Date (Oldest first)",
            VideoSortBy.NAME_ASC to "Name (A to Z)",
            VideoSortBy.NAME_DESC to "Name (Z to A)",
            VideoSortBy.SIZE_LARGEST to "Size (Largest first)",
            VideoSortBy.SIZE_SMALLEST to "Size (Smallest first)",
            VideoSortBy.DURATION_LONGEST to "Duration (Longest first)",
            VideoSortBy.DURATION_SHORTEST to "Duration (Shortest first)"
        )

        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            containerColor = HighDensitySurface,
            title = {
                Text(
                    text = "Sort Videos By",
                    color = HighDensityTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    sortOptions.forEach { (option, label) ->
                        val isSelected = option == uiState.sortBy
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) HighDensityCard else Color.Transparent)
                                .clickable {
                                    viewModel.setSortBy(option)
                                    showSortDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setSortBy(option)
                                    showSortDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = HighDensityBorder
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else HighDensityTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Video Details Dialog
    if (uiState.selectedVideoForDetails != null) {
        VideoDetailsDialog(
            video = uiState.selectedVideoForDetails!!,
            onDismiss = { viewModel.showVideoDetails(null) }
        )
    }
}
