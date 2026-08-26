package com.example.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.data.model.AspectRatioMode
import com.example.data.model.VideoItem
import com.example.data.repository.PlaybackHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class SultanPlayerManager(
    private val context: Context,
    private val historyRepository: PlaybackHistoryRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val audioManager = try {
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    } catch (e: Exception) {
        null
    }

    val equalizerManager = EqualizerManager()

    private val trackSelector = try {
        DefaultTrackSelector(context.applicationContext).apply {
            setParameters(
                buildUponParameters()
                    .setForceHighestSupportedBitrate(true)
                    .setTunnelingEnabled(false)
            )
        }
    } catch (e: Exception) {
        DefaultTrackSelector(context.applicationContext)
    }

    private val renderersFactory = DefaultRenderersFactory(context.applicationContext).apply {
        setEnableDecoderFallback(true)
    }

    val player: ExoPlayer = try {
        ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
    } catch (e: Exception) {
        ExoPlayer.Builder(context.applicationContext).build()
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var gestureHideJob: Job? = null
    private var controlsHideJob: Job? = null

    init {
        // Initialize volume percentage from system safely
        try {
            if (audioManager != null) {
                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val volPct = if (maxVol > 0) ((currentVol.toFloat() / maxVol) * 100).toInt() else 50
                _uiState.update { it.copy(volumePercent = volPct) }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(volumePercent = 50) }
        }

        // Setup BackgroundAudioService Callback Listener
        BackgroundAudioService.playbackActionListener = object : BackgroundAudioService.PlaybackActionListener {
            override fun onPlay() {
                player.play()
            }

            override fun onPause() {
                player.pause()
            }

            override fun onTogglePlay() {
                togglePlayPause()
            }

            override fun onNext() {
                playNext()
            }

            override fun onPrevious() {
                playPrevious()
            }

            override fun onStopPlayback() {
                toggleBackgroundAudio(false)
                player.pause()
            }
        }

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val isEnded = playbackState == Player.STATE_ENDED
                val duration = if (player.duration > 0) player.duration else 0L

                _uiState.update {
                    it.copy(
                        isBuffering = isBuffering,
                        isEnded = isEnded,
                        durationMs = duration
                    )
                }

                if (playbackState == Player.STATE_READY) {
                    startProgressTracker()
                    updateTracksInfo()
                    // Bind AudioSession to Equalizer
                    val audioSessionId = player.audioSessionId
                    if (audioSessionId != 0) {
                        val eqState = equalizerManager.bindAudioSession(audioSessionId)
                        _uiState.update { it.copy(equalizerState = eqState) }
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    saveCurrentProgress()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (_uiState.value.isBackgroundAudio) {
                    val title = _uiState.value.currentVideo?.title ?: "Sultan Video Player"
                    BackgroundAudioService.updateService(context, title, isPlaying)
                }
                if (isPlaying) {
                    startProgressTracker()
                    scheduleControlsHide()
                } else {
                    saveCurrentProgress()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracksInfo()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                _uiState.update {
                    it.copy(
                        videoWidth = videoSize.width,
                        videoHeight = videoSize.height
                    )
                }
            }
        })
    }

    fun playVideo(video: VideoItem, playlist: List<VideoItem> = listOf(video), resumePositionMs: Long? = null) {
        try {
            val index = playlist.indexOfFirst { it.uri == video.uri }.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    currentVideo = video,
                    playlist = playlist,
                    currentIndex = index,
                    currentPositionMs = 0L,
                    durationMs = video.durationMs,
                    isEnded = false,
                    zoomScale = 1.0f,
                    zoomOffsetX = 0f,
                    zoomOffsetY = 0f,
                    abRepeatPointA = null,
                    abRepeatPointB = null,
                    isAbRepeatActive = false
                )
            }

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(video.uri)
                .setMediaId(video.uri.toString())

            // Attach subtitle if available
            if (!video.subtitleUrl.isNullOrBlank()) {
                val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(video.subtitleUrl))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
            }

            val mediaItem = mediaItemBuilder.build()
            player.setMediaItem(mediaItem)
            player.prepare()

            val targetPos = resumePositionMs ?: 0L
            if (targetPos > 0) {
                player.seekTo(targetPos)
            }
            player.play()

            if (_uiState.value.isBackgroundAudio) {
                BackgroundAudioService.updateService(context, video.title, true)
            }

            scheduleControlsHide()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleBackgroundAudio(enable: Boolean? = null) {
        val newState = enable ?: !_uiState.value.isBackgroundAudio
        _uiState.update { it.copy(isBackgroundAudio = newState) }
        val title = _uiState.value.currentVideo?.title ?: "Sultan Video Player"
        if (newState) {
            BackgroundAudioService.startService(context, title, player.isPlaying)
        } else {
            BackgroundAudioService.stopService(context)
        }
    }

    fun togglePlayPause() {
        try {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            player.seekTo(positionMs.coerceIn(0, player.duration.coerceAtLeast(0)))
            _uiState.update { it.copy(currentPositionMs = positionMs) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startScrubbing(positionMs: Long) {
        _uiState.update { it.copy(isSeekingScrub = true, scrubPreviewMs = positionMs) }
    }

    fun updateScrubbing(positionMs: Long) {
        _uiState.update { it.copy(scrubPreviewMs = positionMs) }
    }

    fun commitScrubbing(positionMs: Long) {
        seekTo(positionMs)
        _uiState.update { it.copy(isSeekingScrub = false) }
    }

    fun seekRelative(deltaMs: Long) {
        try {
            val newPos = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
            player.seekTo(newPos)
            _uiState.update { it.copy(currentPositionMs = newPos) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun seekByDelta(deltaSeconds: Int) {
        seekRelative(deltaSeconds * 1000L)
    }

    fun playNext() {
        val playlist = _uiState.value.playlist
        if (playlist.isEmpty()) return
        val nextIdx = _uiState.value.currentIndex + 1
        if (nextIdx < playlist.size) {
            playVideo(playlist[nextIdx], playlist)
        } else if (_uiState.value.repeatMode == 2) {
            playVideo(playlist[0], playlist)
        }
    }

    fun playPrevious() {
        val playlist = _uiState.value.playlist
        if (playlist.isEmpty()) return
        if (player.currentPosition > 3000L) {
            seekTo(0)
            return
        }
        val prevIdx = _uiState.value.currentIndex - 1
        if (prevIdx >= 0 && prevIdx < playlist.size) {
            playVideo(playlist[prevIdx], playlist)
        } else if (_uiState.value.repeatMode == 2) {
            playVideo(playlist[playlist.size - 1], playlist)
        } else {
            seekTo(0)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            player.playbackParameters = PlaybackParameters(speed)
            _uiState.update { it.copy(playbackSpeed = speed) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setAspectRatioMode(mode: AspectRatioMode) {
        _uiState.update { it.copy(aspectRatioMode = mode) }
    }

    fun setRepeatMode(mode: Int) {
        try {
            _uiState.update { it.copy(repeatMode = mode) }
            player.repeatMode = when (mode) {
                1 -> Player.REPEAT_MODE_ONE
                2 -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // A-B Repeat Loop Feature
    fun setAbRepeatPointA(posMs: Long? = null) {
        val currentPos = posMs ?: player.currentPosition
        _uiState.update {
            it.copy(
                abRepeatPointA = currentPos,
                isAbRepeatActive = it.abRepeatPointB != null && it.abRepeatPointB > currentPos
            )
        }
    }

    fun setAbRepeatPointB(posMs: Long? = null) {
        val currentPos = posMs ?: player.currentPosition
        val pointA = _uiState.value.abRepeatPointA
        if (pointA != null && currentPos > pointA) {
            _uiState.update {
                it.copy(
                    abRepeatPointB = currentPos,
                    isAbRepeatActive = true
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    abRepeatPointB = currentPos,
                    isAbRepeatActive = false
                )
            }
        }
    }

    fun clearAbRepeat() {
        _uiState.update {
            it.copy(
                abRepeatPointA = null,
                abRepeatPointB = null,
                isAbRepeatActive = false
            )
        }
    }

    fun toggleAbRepeat() {
        val current = _uiState.value.isAbRepeatActive
        if (current) {
            clearAbRepeat()
        } else {
            setAbRepeatPointA()
        }
    }

    // Equalizer & Audio Booster Controls
    fun setEqualizerEnabled(enabled: Boolean) {
        val newEqState = equalizerManager.setEnabled(enabled)
        _uiState.update { it.copy(equalizerState = newEqState) }
    }

    fun setEqualizerPreset(presetName: String) {
        val newEqState = equalizerManager.applyPreset(presetName)
        _uiState.update { it.copy(equalizerState = newEqState) }
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBel: Int) {
        val newEqState = equalizerManager.setBandLevel(bandIndex, levelMilliBel)
        _uiState.update { it.copy(equalizerState = newEqState) }
    }

    fun setBassBoost(strength: Int) {
        val newEqState = equalizerManager.setBassBoost(strength)
        _uiState.update { it.copy(equalizerState = newEqState) }
    }

    fun setVolumeBoost(percent: Int) {
        val newEqState = equalizerManager.setVolumeBoost(percent)
        _uiState.update { it.copy(equalizerState = newEqState) }
    }

    fun setReverb(preset: Short) {
        val newEqState = equalizerManager.setReverb(preset)
        _uiState.update { it.copy(equalizerState = newEqState) }
    }

    // Subtitle Attachment
    fun loadExternalSubtitle(context: Context? = null, subtitleUri: Uri, label: String = "External Subtitle") {
        try {
            val video = _uiState.value.currentVideo ?: return
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                .setMimeType(if (subtitleUri.toString().endsWith(".srt", true)) MimeTypes.APPLICATION_SUBRIP else MimeTypes.TEXT_VTT)
                .setLabel(label)
                .setLanguage("en")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()

            val curPos = player.currentPosition
            val mediaItem = MediaItem.Builder()
                .setUri(video.uri)
                .setMediaId(video.uri.toString())
                .setSubtitleConfigurations(listOf(subtitleConfig))
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.seekTo(curPos)
            player.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleScreenLock() {
        val newLock = !_uiState.value.isLocked
        _uiState.update { it.copy(isLocked = newLock, isControlsVisible = !newLock) }
    }

    fun setScreenLocked(locked: Boolean) {
        _uiState.update { it.copy(isLocked = locked, isControlsVisible = !locked) }
    }

    fun toggleControlsVisibility() {
        val newVisible = !_uiState.value.isControlsVisible
        _uiState.update { it.copy(isControlsVisible = newVisible) }
        if (newVisible) {
            scheduleControlsHide()
        }
    }

    fun setControlsVisibility(visible: Boolean) {
        _uiState.update { it.copy(isControlsVisible = visible) }
        if (visible) {
            scheduleControlsHide()
        }
    }

    fun scheduleControlsHide() {
        controlsHideJob?.cancel()
        controlsHideJob = scope.launch {
            delay(4000)
            if (_uiState.value.isPlaying && !_uiState.value.isLocked) {
                _uiState.update { it.copy(isControlsVisible = false) }
            }
        }
    }

    fun setZoom(scale: Float, offsetX: Float, offsetY: Float) {
        _uiState.update {
            it.copy(
                zoomScale = scale,
                zoomOffsetX = offsetX,
                zoomOffsetY = offsetY
            )
        }
    }

    fun resetZoom() {
        _uiState.update {
            it.copy(
                zoomScale = 1.0f,
                zoomOffsetX = 0f,
                zoomOffsetY = 0f
            )
        }
    }

    fun adjustVolumeDelta(deltaAmount: Float) {
        try {
            val current = _uiState.value.volumePercent
            val newPct = (current + deltaAmount.toInt()).coerceIn(0, 100)
            _uiState.update { it.copy(volumePercent = newPct, activeGesture = ActiveGestureType.VOLUME, gestureValue = newPct / 100f) }

            if (audioManager != null) {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = ((newPct.toFloat() / 100f) * maxVol).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            }
            scheduleGestureHide()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun adjustBrightnessDelta(activity: Activity?, deltaAmount: Float) {
        try {
            val current = _uiState.value.brightnessPercent
            val newPct = (current + deltaAmount.toInt()).coerceIn(0, 100)
            _uiState.update { it.copy(brightnessPercent = newPct, activeGesture = ActiveGestureType.BRIGHTNESS, gestureValue = newPct / 100f) }

            activity?.let {
                val layoutParams = it.window.attributes
                layoutParams.screenBrightness = (newPct / 100f).coerceIn(0.01f, 1.0f)
                it.window.attributes = layoutParams
            }
            scheduleGestureHide()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSeekGesture(deltaSec: Int) {
        try {
            val totalDurationMs = if (player.duration > 0) player.duration else _uiState.value.durationMs
            val currentPos = player.currentPosition
            val target = (currentPos + deltaSec * 1000L).coerceIn(0L, totalDurationMs.coerceAtLeast(1000L))
            _uiState.update {
                it.copy(
                    activeGesture = ActiveGestureType.SEEK,
                    seekDeltaSeconds = deltaSec,
                    seekTargetMs = target,
                    gestureValue = if (totalDurationMs > 0) target.toFloat() / totalDurationMs else 0f
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun commitSeekGesture() {
        try {
            if (_uiState.value.activeGesture == ActiveGestureType.SEEK) {
                val targetMs = _uiState.value.seekTargetMs
                seekTo(targetMs)
            }
            _uiState.update { it.copy(activeGesture = ActiveGestureType.NONE, seekDeltaSeconds = 0) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearGesture() {
        _uiState.update { it.copy(activeGesture = ActiveGestureType.NONE) }
    }

    private fun scheduleGestureHide() {
        gestureHideJob?.cancel()
        gestureHideJob = scope.launch {
            delay(1500)
            clearGesture()
        }
    }

    fun triggerDoubleTapSeek(side: SeekSide) {
        try {
            val delta = if (side == SeekSide.LEFT) -10000L else 10000L
            seekRelative(delta)
            _uiState.update { it.copy(doubleTapSeekSide = side) }
            scope.launch {
                delay(600)
                _uiState.update { it.copy(doubleTapSeekSide = null) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectAudioTrack(track: MediaTrackItem?) {
        try {
            if (track == null) return
            val tracks = player.currentTracks
            val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            if (track.groupIndex in audioGroups.indices) {
                val group = audioGroups[track.groupIndex]
                val parameters = player.trackSelectionParameters
                    .buildUpon()
                    .setOverrideForType(
                        TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                    )
                    .build()
                player.trackSelectionParameters = parameters
                _uiState.update { it.copy(selectedAudioTrackIndex = track.trackIndex) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectSubtitleTrack(track: MediaTrackItem?) {
        try {
            if (track == null) {
                // Disable subtitles
                val parameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build()
                player.trackSelectionParameters = parameters
                _uiState.update { it.copy(selectedSubtitleTrackIndex = -1) }
                return
            }

            val tracks = player.currentTracks
            val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
            if (track.groupIndex in textGroups.indices) {
                val group = textGroups[track.groupIndex]
                val parameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(
                        TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
                    )
                    .build()
                player.trackSelectionParameters = parameters
                _uiState.update { it.copy(selectedSubtitleTrackIndex = track.trackIndex) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setSubtitleDelay(delayMs: Long) {
        _uiState.update { it.copy(subtitleDelayMs = delayMs) }
    }

    fun setSubtitleSize(sizeSp: Int) {
        _uiState.update { it.copy(subtitleTextSizeSp = sizeSp) }
    }

    private fun updateTracksInfo() {
        try {
            val tracks = player.currentTracks
            val audioList = mutableListOf<MediaTrackItem>()
            val subList = mutableListOf<MediaTrackItem>()

            tracks.groups.forEachIndexed { gIdx, group ->
                for (tIdx in 0 until group.length) {
                    val format = group.getTrackFormat(tIdx)
                    val isSelected = group.isTrackSelected(tIdx)
                    val label = format.label ?: format.language ?: "Track ${tIdx + 1}"
                    val mime = format.sampleMimeType ?: ""

                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        audioList.add(
                            MediaTrackItem(
                                id = "$gIdx-$tIdx",
                                name = label,
                                language = format.language,
                                format = mime,
                                groupIndex = gIdx,
                                trackIndex = tIdx,
                                isSelected = isSelected
                            )
                        )
                    } else if (group.type == C.TRACK_TYPE_TEXT) {
                        subList.add(
                            MediaTrackItem(
                                id = "$gIdx-$tIdx",
                                name = label,
                                language = format.language,
                                format = mime,
                                groupIndex = gIdx,
                                trackIndex = tIdx,
                                isSelected = isSelected
                            )
                        )
                    }
                }
            }

            _uiState.update {
                it.copy(
                    audioTracks = audioList,
                    subtitleTracks = subList
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                try {
                    val currentPos = player.currentPosition
                    val duration = if (player.duration > 0) player.duration else _uiState.value.durationMs
                    val buffered = player.bufferedPosition

                    // A-B Repeat Loop Check
                    val state = _uiState.value
                    if (state.isAbRepeatActive && state.abRepeatPointA != null && state.abRepeatPointB != null) {
                        if (currentPos >= state.abRepeatPointB) {
                            player.seekTo(state.abRepeatPointA)
                        }
                    }

                    _uiState.update {
                        it.copy(
                            currentPositionMs = currentPos,
                            durationMs = duration,
                            bufferedPositionMs = buffered
                        )
                    }
                } catch (e: Exception) {
                    // Ignore transient tracker errors
                }
                delay(400)
            }
        }
    }

    fun saveHistoryPosition() {
        saveCurrentProgress()
    }

    fun saveCurrentProgress() {
        try {
            val video = _uiState.value.currentVideo ?: return
            val currentPos = player.currentPosition
            val dur = if (player.duration > 0) player.duration else video.durationMs
            if (dur > 0) {
                scope.launch(Dispatchers.IO) {
                    historyRepository.saveProgress(
                        uri = video.uri.toString(),
                        title = video.title,
                        positionMs = currentPos,
                        durationMs = dur
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            saveCurrentProgress()
            equalizerManager.release()
            progressJob?.cancel()
            gestureHideJob?.cancel()
            controlsHideJob?.cancel()
            player.stop()
            player.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
