package com.aozijx.passly.core.media

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.aozijx.passly.core.logging.Logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FaviconUtils {

    private const val TAG = "FaviconUtils"

    enum class DownloadResult {
        SUCCESS,
        NETWORK_ERROR,
        DECODE_ERROR,
        SAVE_ERROR,
        EMPTY_INPUT
    }

    data class DownloadOutcome(
        val result: DownloadResult,
        val filePath: String? = null
    )

    suspend fun downloadAndSaveFavicon(input: String, context: Context): DownloadOutcome = withContext(Dispatchers.IO) {
        if (input.isBlank()) return@withContext DownloadOutcome(DownloadResult.EMPTY_INPUT)

        Logcat.d(TAG, "Trying to download favicon from: $input")

        val isDirectUrl = input.startsWith("http://") || input.startsWith("https://")

        if (isDirectUrl) {
            Logcat.d(TAG, "Input is a direct URL, downloading directly")
            val bitmap = downloadFaviconWithCoil(input, context)
            if (bitmap != null) {
                val savedPath = saveBitmapToInternalStorage(context, bitmap)
                return@withContext if (savedPath != null) {
                    Logcat.d(TAG, "Successfully downloaded favicon from direct URL")
                    DownloadOutcome(DownloadResult.SUCCESS, savedPath)
                } else {
                    DownloadOutcome(DownloadResult.SAVE_ERROR)
                }
            }
            return@withContext DownloadOutcome(DownloadResult.NETWORK_ERROR)
        } else {
            Logcat.d(TAG, "Input is a domain, trying favicon paths")
            val cleanDomain = cleanDomain(input)
            if (cleanDomain.isBlank()) return@withContext DownloadOutcome(DownloadResult.EMPTY_INPUT)

            val faviconUrls = listOf(
                "https://$cleanDomain/favicon.ico",
                "https://$cleanDomain/favicon.png"
            )

            for (url in faviconUrls) {
                try {
                    Logcat.d(TAG, "Trying: $url")
                    val bitmap = downloadFaviconWithCoil(url, context)
                    if (bitmap != null) {
                        val savedPath = saveBitmapToInternalStorage(context, bitmap)
                        if (savedPath != null) {
                            Logcat.d(TAG, "Successfully downloaded favicon from: $url")
                            return@withContext DownloadOutcome(DownloadResult.SUCCESS, savedPath)
                        }
                    }
                } catch (e: Exception) {
                    Logcat.e(TAG, "Failed to download from $url", e)
                }
            }
        }

        Logcat.w(TAG, "Failed to download favicon from: $input")
        DownloadOutcome(DownloadResult.NETWORK_ERROR)
    }

    private fun cleanDomain(domain: String): String {
        var clean = domain.trim()
        clean = clean.removePrefix("http://")
        clean = clean.removePrefix("https://")
        clean = clean.split("/").firstOrNull() ?: ""
        clean = clean.split(":").firstOrNull() ?: ""
        return clean
    }

    private fun downloadFaviconWithCoil(urlString: String, context: Context): Bitmap? {
        return try {
            val imageLoader = ImageLoader.Builder(context)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()

            val request = ImageRequest.Builder(context)
                .data(urlString)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val result = runBlocking {
                imageLoader.execute(request)
            }

            val drawable = result.drawable
            drawable?.toBitmap()
        } catch (e: Exception) {
            Logcat.e(TAG, "Error downloading favicon with Coil from $urlString", e)
            null
        }
    }

    private fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val directory = File(context.filesDir, "vault_images").apply {
                if (!exists()) mkdirs()
            }

            val fileName = "favicon_${UUID.randomUUID()}.png"
            val destFile = File(directory, fileName)

            FileOutputStream(destFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }

            Logcat.d(TAG, "Favicon saved to: ${destFile.absolutePath}")
            destFile.absolutePath
        } catch (e: Exception) {
            Logcat.e(TAG, "Error saving favicon to storage", e)
            null
        }
    }
}


