package com.example.player

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File

/**
 * Actually trims a video to [startMs, endMs] and writes an MP4 to the device's
 * Movies folder using Media3's Transformer.
 *
 * This replaces the previous "Video Cutter" implementation, which only showed a
 * fake progress spinner for ~1.2s and then claimed success without producing any
 * output file at all.
 */
@OptIn(UnstableApi::class)
object VideoTrimmer {

    sealed interface Result {
        data class Success(val outputUri: Uri) : Result
        data class Failure(val message: String) : Result
    }

    fun trim(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFileName: String,
        onProgress: (Boolean) -> Unit,
        onComplete: (Result) -> Unit
    ) {
        val safeStart = startMs.coerceAtLeast(0L)
        val safeEnd = endMs.coerceAtLeast(safeStart + 500L)

        val outputFile = File(context.cacheDir, outputFileName)
        if (outputFile.exists()) outputFile.delete()

        val clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(safeStart)
            .setEndPositionMs(safeEnd)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(clippingConfiguration)
            .build()

        val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()
        val composition = Composition.Builder(EditedMediaItemSequence.Builder(editedMediaItem).build()).build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onProgress(false)
                    val savedUri = saveToMoviesFolder(context, outputFile, outputFileName)
                    if (savedUri != null) {
                        onComplete(Result.Success(savedUri))
                    } else {
                        onComplete(Result.Failure("Trim succeeded but saving the file failed."))
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    onProgress(false)
                    onComplete(Result.Failure(exportException.message ?: "Failed to trim video."))
                }
            })
            .build()

        onProgress(true)
        try {
            transformer.start(composition, outputFile.absolutePath)
        } catch (e: Exception) {
            onProgress(false)
            onComplete(Result.Failure(e.message ?: "Failed to start trimming."))
        }
    }

    /** Copies the trimmed file from the app's cache dir into the public Movies folder. */
    private fun saveToMoviesFolder(context: Context, sourceFile: File, displayName: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SultanCuts")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values) ?: return null
                resolver.openOutputStream(itemUri)?.use { out ->
                    sourceFile.inputStream().use { input -> input.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                sourceFile.delete()
                itemUri
            } else {
                @Suppress("DEPRECATION")
                val moviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "SultanCuts")
                if (!moviesDir.exists()) moviesDir.mkdirs()
                val destFile = File(moviesDir, displayName)
                sourceFile.copyTo(destFile, overwrite = true)
                sourceFile.delete()
                Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            null
        }
    }
}
