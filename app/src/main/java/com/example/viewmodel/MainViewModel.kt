package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.FavoriteVideo
import com.example.data.model.NetworkStreamItem
import com.example.data.model.PlaybackHistory
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import com.example.data.model.VideoSortBy
import android.content.IntentSender
import com.example.data.repository.DeleteResult
import com.example.data.repository.RenameResult
import com.example.data.repository.PlaybackHistoryRepository
import com.example.data.repository.StreamHistoryRepository
import com.example.data.repository.VaultRepository
import com.example.data.repository.VideoRepository
import com.example.ui.theme.AppThemePalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
    FOLDERS,
    ALL_VIDEOS,
    NETWORK_STREAM,
    VAULT,
    HISTORY,
    FAVORITES,
    SETTINGS
}

data class MainUiState(
    val currentTab: MainTab = MainTab.FOLDERS,
    val selectedFolder: VideoFolder? = null,
    val videos: List<VideoItem> = emptyList(),
    val folders: List<VideoFolder> = emptyList(),
    val searchQuery: String = "",
    val sortBy: VideoSortBy = VideoSortBy.DATE_NEWEST,
    val isLoading: Boolean = true,
    val selectedVideoForDetails: VideoItem? = null,
    val selectedPalette: AppThemePalette = AppThemePalette.CYBER_VIOLET
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val historyRepository = PlaybackHistoryRepository(db.playbackHistoryDao(), db.favoriteDao())
    val videoRepository = VideoRepository(application)
    val vaultRepository = VaultRepository(application)
    val streamHistoryRepository = StreamHistoryRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val historyList: StateFlow<List<PlaybackHistory>> = historyRepository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesList: StateFlow<List<FavoriteVideo>> = historyRepository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultVideos: StateFlow<List<VideoItem>> = vaultRepository.vaultVideosFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val streamHistory: StateFlow<List<NetworkStreamItem>> = streamHistoryRepository.streamsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var loadJob: kotlinx.coroutines.Job? = null
    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        loadMedia()
    }

    fun setTab(tab: MainTab) {
        _uiState.update { it.copy(currentTab = tab, selectedFolder = null) }
    }

    fun selectFolder(folder: VideoFolder?) {
        _uiState.update { it.copy(selectedFolder = folder) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(180)
            loadMediaInternal()
        }
    }

    fun setSortBy(sortBy: VideoSortBy) {
        _uiState.update { it.copy(sortBy = sortBy) }
        loadMedia()
    }

    fun setThemePalette(palette: AppThemePalette) {
        _uiState.update { it.copy(selectedPalette = palette) }
    }

    fun showVideoDetails(video: VideoItem?) {
        _uiState.update { it.copy(selectedVideoForDetails = video) }
    }

    fun loadMedia() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            loadMediaInternal()
        }
    }

    private suspend fun loadMediaInternal() {
        _uiState.update { it.copy(isLoading = it.videos.isEmpty()) }
        val state = _uiState.value
        val allVideos = videoRepository.loadVideos(sortBy = state.sortBy, searchQuery = state.searchQuery)
        val folders = videoRepository.getFoldersFromVideos(allVideos)

        _uiState.update {
            it.copy(
                videos = allVideos,
                folders = folders,
                isLoading = false
            )
        }
    }

    fun addVideoToVault(video: VideoItem) {
        viewModelScope.launch {
            vaultRepository.addVideoToVault(video)
        }
    }

    fun removeVideoFromVault(video: VideoItem) {
        viewModelScope.launch {
            vaultRepository.removeVideoFromVault(video.uri.toString())
        }
    }

    fun saveNetworkStream(url: String, title: String) {
        viewModelScope.launch {
            streamHistoryRepository.saveStream(url, title)
        }
    }

    fun deleteNetworkStream(url: String) {
        viewModelScope.launch {
            streamHistoryRepository.deleteStream(url)
        }
    }

    private val _deletePendingIntent = MutableStateFlow<IntentSender?>(null)
    val deletePendingIntent: StateFlow<IntentSender?> = _deletePendingIntent.asStateFlow()

    private var pendingDeleteVideo: VideoItem? = null

    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            when (val result = videoRepository.deleteVideo(video)) {
                is DeleteResult.Success -> {
                    onVideoDeleted(video)
                }
                is DeleteResult.RequiresIntentSender -> {
                    pendingDeleteVideo = result.video
                    _deletePendingIntent.value = result.intentSender
                }
                is DeleteResult.Error -> {
                    onVideoDeleted(video)
                }
            }
        }
    }

    fun onPendingDeleteSuccess() {
        pendingDeleteVideo?.let { video ->
            onVideoDeleted(video)
            pendingDeleteVideo = null
        }
        _deletePendingIntent.value = null
    }

    fun onPendingDeleteCanceled() {
        pendingDeleteVideo = null
        _deletePendingIntent.value = null
    }

    private fun onVideoDeleted(video: VideoItem) {
        viewModelScope.launch {
            historyRepository.deleteHistory(video.uri.toString())
            vaultRepository.removeVideoFromVault(video.uri.toString())
            _uiState.update { current ->
                val updatedVideos = current.videos.filterNot { it.id == video.id || it.uri == video.uri }
                val updatedFolders = videoRepository.getFoldersFromVideos(updatedVideos)
                current.copy(videos = updatedVideos, folders = updatedFolders)
            }
            loadMedia()
        }
    }

    private val _renamePendingIntent = MutableStateFlow<IntentSender?>(null)
    val renamePendingIntent: StateFlow<IntentSender?> = _renamePendingIntent.asStateFlow()

    private var pendingRenameVideo: VideoItem? = null
    private var pendingRenameName: String? = null

    fun renameVideo(video: VideoItem, newName: String) {
        viewModelScope.launch {
            when (val result = videoRepository.renameVideo(video, newName)) {
                is RenameResult.Success -> {
                    onVideoRenamed()
                }
                is RenameResult.RequiresIntentSender -> {
                    pendingRenameVideo = result.video
                    pendingRenameName = result.newName
                    _renamePendingIntent.value = result.intentSender
                }
                is RenameResult.Error -> {
                    onVideoRenamed()
                }
            }
        }
    }

    fun onPendingRenameSuccess() {
        val video = pendingRenameVideo
        val name = pendingRenameName
        if (video != null && name != null) {
            viewModelScope.launch {
                videoRepository.renameVideo(video, name)
                onVideoRenamed()
            }
        } else {
            onVideoRenamed()
        }
        pendingRenameVideo = null
        pendingRenameName = null
        _renamePendingIntent.value = null
    }

    fun onPendingRenameCanceled() {
        pendingRenameVideo = null
        pendingRenameName = null
        _renamePendingIntent.value = null
    }

    private fun onVideoRenamed() {
        viewModelScope.launch {
            loadMedia()
        }
    }

    fun toggleFavorite(video: VideoItem, currentIsFavorite: Boolean) {
        viewModelScope.launch {
            historyRepository.toggleFavorite(video.uri.toString(), video.title, currentIsFavorite)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }
}
