package com.aozijx.passly.app.entry.favicon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class FaviconBitmapTransformer @Inject constructor() {
    fun transform(input: File, output: File, crop: FaviconCropRequest?) {
        validateBounds(input)
        val decoded = try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(input)) { decoder, info, _ ->
                if (info.size.width !in 1..MAX_SOURCE_EDGE || info.size.height !in 1..MAX_SOURCE_EDGE) {
                    throw FaviconImageException(FaviconImageFailure.UNSUPPORTED_DIMENSIONS)
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (error: FaviconImageException) {
            throw error
        } catch (_: Exception) {
            throw FaviconImageException(FaviconImageFailure.DECODE_FAILED)
        }
        val transformed = try {
            if (crop == null) decoded.scaledToMaxEdge(NO_CROP_MAX_EDGE) else decoded.squareCrop(crop)
        } finally {
            if (!decoded.isRecycled) decoded.recycle()
        }
        try {
            FileOutputStream(output).use { stream ->
                val format = if (transformed.hasAlpha()) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    Bitmap.CompressFormat.WEBP_LOSSY
                }
                if (!transformed.compress(format, if (transformed.hasAlpha()) 100 else 90, stream)) {
                    throw FaviconImageException(FaviconImageFailure.ENCODE_FAILED)
                }
            }
        } catch (error: Throwable) {
            output.delete()
            throw error
        } finally {
            transformed.recycle()
        }
    }

    private fun validateBounds(input: File) {
        val bounds = BitmapFactory.Options().also { options ->
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(input.absolutePath, options)
        }
        if (bounds.outWidth !in 1..MAX_SOURCE_EDGE || bounds.outHeight !in 1..MAX_SOURCE_EDGE) {
            throw FaviconImageException(FaviconImageFailure.UNSUPPORTED_DIMENSIONS)
        }
    }

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

    private companion object {
        const val MAX_SOURCE_EDGE = 4096
        const val NO_CROP_MAX_EDGE = 1024
        const val CROP_SIZE = 512
        const val MAX_ZOOM = 6f
    }
}
