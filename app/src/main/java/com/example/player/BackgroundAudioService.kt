package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class BackgroundAudioService : Service() {

    interface PlaybackActionListener {
        fun onPlay()
        fun onPause()
        fun onTogglePlay()
        fun onNext()
        fun onPrevious()
        fun onStopPlayback()
    }

    companion object {
        const val CHANNEL_ID = "sultan_bg_audio_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.action.START_BG_AUDIO"
        const val ACTION_UPDATE = "com.example.action.UPDATE_BG_AUDIO"
        const val ACTION_STOP = "com.example.action.STOP_BG_AUDIO"
        const val ACTION_TOGGLE_PLAY = "com.example.action.TOGGLE_PLAY"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_NEXT = "com.example.action.NEXT"

        const val EXTRA_VIDEO_TITLE = "extra_video_title"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        var playbackActionListener: PlaybackActionListener? = null

        fun startService(context: Context, videoTitle: String, isPlaying: Boolean) {
            val intent = Intent(context, BackgroundAudioService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateService(context: Context, videoTitle: String, isPlaying: Boolean) {
            val intent = Intent(context, BackgroundAudioService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BackgroundAudioService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var currentTitle: String = "Sultan Video Player"
    private var isCurrentlyPlaying: Boolean = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Sultan Video Player"
                isCurrentlyPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                startForeground(NOTIFICATION_ID, buildNotification(currentTitle, isCurrentlyPlaying))
            }
            ACTION_UPDATE -> {
                currentTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: currentTitle
                isCurrentlyPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isCurrentlyPlaying)
                val notification = buildNotification(currentTitle, isCurrentlyPlaying)
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                playbackActionListener?.onStopPlayback()
                stopForeground(true)
                stopSelf()
            }
            ACTION_TOGGLE_PLAY -> {
                playbackActionListener?.onTogglePlay()
                isCurrentlyPlaying = !isCurrentlyPlaying
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(NOTIFICATION_ID, buildNotification(currentTitle, isCurrentlyPlaying))
            }
            ACTION_PREV -> {
                playbackActionListener?.onPrevious()
            }
            ACTION_NEXT -> {
                playbackActionListener?.onNext()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(title: String, isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingActivityIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Prev Action
        val prevIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getService(this, 10, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Toggle Play Action
        val toggleIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_TOGGLE_PLAY }
        val togglePending = PendingIntent.getService(this, 20, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Next Action
        val nextIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 30, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Stop Action
        val stopIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 40, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "Playing in Background • Screen-off Play Active" else "Audio Paused")
            .setSubText("Sultan Background Audio")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingActivityIntent)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(playPauseIcon, playPauseTitle, togglePending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPending)
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Sultan Background Audio Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Persistent controls for background and screen-off media playback"
                    setShowBadge(false)
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(channel)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
