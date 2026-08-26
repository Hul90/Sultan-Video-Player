package com.example.player

import com.example.data.model.AspectRatioMode
import com.example.data.model.VideoItem

data class MediaTrackItem(
    val id: String,
    val name: String,
    val language: String?,
    val format: String?,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean = false
)

data class PlayerUiState(
    val currentVideo: VideoItem? = null,
    val playlist: List<VideoItem> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val volumePercent: Int = 50,
    val brightnessPercent: Int = 50,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val isBuffering: Boolean = false,
    val isEnded: Boolean = false,
    val isLocked: Boolean = false,
    val isPipMode: Boolean = false,
    val isBackgroundAudio: Boolean = false,
    val repeatMode: Int = 0, // 0: OFF, 1: ONE, 2: ALL
    val audioTracks: List<MediaTrackItem> = emptyList(),
    val selectedAudioTrackIndex: Int = -1,
    val subtitleTracks: List<MediaTrackItem> = emptyList(),
    val selectedSubtitleTrackIndex: Int = -1,
    val subtitleDelayMs: Long = 0L,
    val subtitleTextSizeSp: Int = 18,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val isControlsVisible: Boolean = true,
    val activeGesture: ActiveGestureType = ActiveGestureType.NONE,
    val gestureValue: Float = 0f,
    val seekTargetMs: Long = 0L,
    val seekDeltaSeconds: Int = 0,
    val doubleTapSeekSide: SeekSide? = null,
    val zoomScale: Float = 1.0f,
    val zoomOffsetX: Float = 0f,
    val zoomOffsetY: Float = 0f,
    // Advanced features
    val abRepeatPointA: Long? = null,
    val abRepeatPointB: Long? = null,
    val isAbRepeatActive: Boolean = false,
    val equalizerState: EqualizerState = EqualizerState(),
    val isSeekingScrub: Boolean = false,
    val scrubPreviewMs: Long = 0L
) {
    val isBackgroundAudioActive: Boolean get() = isBackgroundAudio
}

enum class ActiveGestureType {
    NONE,
    VOLUME,
    BRIGHTNESS,
    SEEK
}

enum class SeekSide {
    LEFT,
    RIGHT
}
