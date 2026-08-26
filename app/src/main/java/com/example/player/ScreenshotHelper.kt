package com.example.player

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ScreenshotHelper {

    suspend fun saveScreenshot(context: Context, bitmap: Bitmap, videoTitle: String): Uri? =
        withContext(Dispatchers.IO) {
            val fileName = "Sultan_${System.currentTimeMillis()}.jpg"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/SultanPlayer")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    uri?.let {
                        val out: OutputStream? = context.contentResolver.openOutputStream(it)
                        out?.use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                        }
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(it, values, null, null)
                    }
                    return@withContext uri
                } else {
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val sultanDir = File(picturesDir, "SultanPlayer")
                    if (!sultanDir.exists()) sultanDir.mkdirs()

                    val file = File(sultanDir, fileName)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DATA, file.absolutePath)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    }
                    return@withContext context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
}
