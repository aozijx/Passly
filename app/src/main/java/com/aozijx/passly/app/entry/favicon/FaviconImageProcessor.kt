package com.aozijx.passly.app.entry.favicon

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.aozijx.passly.core.platform.VaultResourcePaths
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

data class FaviconCropRequest(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

@Singleton
class FaviconImageProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun stageUpload(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                writeBoundedInput(input.readBytesBounded(), RAW_EXTENSION)
            } ?: error("Unable to open selected image")
        }
    }

    suspend fun stageHttpsUrl(value: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            var current = validatePublicHttps(value)
            repeat(MAX_REDIRECTS + 1) { redirectIndex ->
                val connection = (current.toURL().openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("Accept", "image/*")
                }
                try {
                    val status = connection.responseCode
                    if (status in 300..399) {
                        check(redirectIndex < MAX_REDIRECTS) { "Too many redirects" }
                        val location = connection.getHeaderField("Location") ?: error("Invalid redirect")
                        current = validatePublicHttps(current.resolve(location).toString())
                    } else {
                        check(status in 200..299) { "Image download failed" }
                        val contentType = connection.contentType.orEmpty().lowercase()
                        check(contentType.startsWith("image/")) { "URL is not an image" }
                        return@runCatching connection.inputStream.use { input ->
                            writeBoundedInput(input.readBytesBounded(), RAW_EXTENSION)
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }
            error("Too many redirects")
        }
    }

    suspend fun process(
        stagedInputPath: String,
        crop: FaviconCropRequest?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val input = requireStagingFile(stagedInputPath)
            val bounds = BitmapFactory.Options().also { options ->
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(input.absolutePath, options)
            }
            check(bounds.outWidth in 1..MAX_SOURCE_EDGE && bounds.outHeight in 1..MAX_SOURCE_EDGE) {
                "Unsupported image dimensions"
            }
            val decoded = BitmapFactory.decodeFile(input.absolutePath) ?: error("Unable to decode image")
            val output = try {
                if (crop == null) decoded.scaledToMaxEdge(NO_CROP_MAX_EDGE) else decoded.squareCrop(crop)
            } finally {
                if (!decoded.isRecycled) decoded.recycle()
            }
            val outputFile = stagingFile(WEBP_EXTENSION)
            try {
                FileOutputStream(outputFile).use { stream ->
                    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && output.hasAlpha()) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                    }
                    check(output.compress(format, if (output.hasAlpha()) 100 else 90, stream))
                }
            } finally {
                output.recycle()
            }
            input.delete()
            outputFile.absolutePath
        }
    }

    suspend fun promote(stagedPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val source = requireStagingFile(stagedPath)
            val root = VaultResourcePaths.vaultImagesDir(context).apply { mkdirs() }.canonicalFile
            val target = File(root, "favicon_${UUID.randomUUID()}.webp").canonicalFile
            check(target.parentFile == root)
            check(source.renameTo(target)) { "Unable to save favicon" }
            target.absolutePath
        }
    }

    suspend fun discard(path: String?) = withContext(Dispatchers.IO) {
        path ?: return@withContext
        runCatching { requireStagingFile(path).delete() }
    }

    suspend fun discardPromotedCandidate(path: String?) = withContext(Dispatchers.IO) {
        path ?: return@withContext
        runCatching {
            val root = VaultResourcePaths.vaultImagesDir(context).canonicalFile
            val target = File(path).canonicalFile
            if (
                target.parentFile == root &&
                target.name.startsWith("favicon_") &&
                target.extension.equals("webp", ignoreCase = true)
            ) {
                target.delete()
            }
        }
    }

    fun isStaged(path: String): Boolean = runCatching {
        requireStagingFile(path).isFile
    }.getOrDefault(false)

    private fun Bitmap.squareCrop(request: FaviconCropRequest): Bitmap {
        val baseSide = min(width, height).toFloat()
        val zoom = request.zoom.coerceIn(1f, MAX_ZOOM)
        val sourceSide = (baseSide / zoom).toInt().coerceAtLeast(1)
        val availableX = (width - sourceSide).coerceAtLeast(0)
        val availableY = (height - sourceSide).coerceAtLeast(0)
        val left = (((-request.offsetX.coerceIn(-1f, 1f) + 1f) / 2f) * availableX).toInt()
        val top = (((-request.offsetY.coerceIn(-1f, 1f) + 1f) / 2f) * availableY).toInt()
        val cropped = Bitmap.createBitmap(this, left, top, sourceSide, sourceSide)
        return Bitmap.createScaledBitmap(cropped, CROP_SIZE, CROP_SIZE, true).also {
            if (it !== cropped) cropped.recycle()
        }
    }

    private fun Bitmap.scaledToMaxEdge(maxEdge: Int): Bitmap {
        val edge = max(width, height)
        if (edge <= maxEdge) return copy(config ?: Bitmap.Config.ARGB_8888, false)
        val ratio = maxEdge.toFloat() / edge
        return Bitmap.createScaledBitmap(this, (width * ratio).toInt(), (height * ratio).toInt(), true)
    }

    private fun validatePublicHttps(value: String): URI {
        val uri = URI(value.trim())
        check(uri.scheme.equals("https", ignoreCase = true) && uri.host != null && uri.userInfo == null)
        val host = uri.host.lowercase()
        check(host != "localhost" && !host.endsWith(".local") && !host.endsWith(".internal") && !host.endsWith(".lan"))
        InetAddress.getAllByName(host).forEach { address ->
            check(!address.isAnyLocalAddress && !address.isLoopbackAddress && !address.isLinkLocalAddress &&
                !address.isSiteLocalAddress && !address.isMulticastAddress) { "Private address is not allowed" }
        }
        return uri
    }

    private fun java.io.InputStream.readBytesBounded(): ByteArray {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val output = java.io.ByteArrayOutputStream()
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            check(total <= MAX_INPUT_BYTES) { "Image is too large" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun writeBoundedInput(bytes: ByteArray, extension: String): String {
        check(bytes.isNotEmpty() && bytes.size <= MAX_INPUT_BYTES)
        return stagingFile(extension).also { it.writeBytes(bytes) }.absolutePath
    }

    private fun stagingFile(extension: String): File = File(
        stagingRoot().apply { mkdirs() },
        "${UUID.randomUUID()}$extension",
    )

    private fun requireStagingFile(path: String): File {
        val root = stagingRoot().apply { mkdirs() }.canonicalFile
        val target = File(path).canonicalFile
        check(target.parentFile == root) { "Invalid staging path" }
        check(target.isFile) { "Missing staged image" }
        return target
    }

    private fun stagingRoot() = File(VaultResourcePaths.vaultImagesDir(context), STAGING_DIRECTORY)

    private companion object {
        const val STAGING_DIRECTORY = ".staging"
        const val RAW_EXTENSION = ".input"
        const val WEBP_EXTENSION = ".webp"
        const val MAX_INPUT_BYTES = 10 * 1024 * 1024
        const val MAX_SOURCE_EDGE = 4096
        const val NO_CROP_MAX_EDGE = 1024
        const val CROP_SIZE = 512
        const val MAX_ZOOM = 6f
        const val MAX_REDIRECTS = 5
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
    }
}
