package com.aozijx.passly.app.entry.favicon

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class FaviconCropRequest(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

@Singleton
class FaviconImageProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageDownloader: FaviconImageDownloader,
    private val imageStore: FaviconImageStore,
    private val bitmapTransformer: FaviconBitmapTransformer,
) {
    suspend fun stageUpload(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                imageStore.stage(input)
            } ?: throw FaviconImageException(FaviconImageFailure.DECODE_FAILED)
        }
    }

    suspend fun stageHttpsUrl(value: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ByteArrayInputStream(imageDownloader.download(value)).use(imageStore::stage)
        }
    }

    suspend fun process(
        stagedInputPath: String,
        crop: FaviconCropRequest?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val input = imageStore.requireStaged(stagedInputPath)
            val output = imageStore.createTransformOutput()
            try {
                bitmapTransformer.transform(input, output, crop)
                imageStore.finishTransform(input, output)
            } catch (error: Throwable) {
                output.delete()
                throw error
            }
        }
    }

    suspend fun promote(stagedPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            imageStore.promote(stagedPath)
        }
    }

    suspend fun discard(path: String?) = withContext(Dispatchers.IO) {
        path ?: return@withContext
        imageStore.discard(path)
    }

    suspend fun discardPromotedCandidate(path: String?) = withContext(Dispatchers.IO) {
        path ?: return@withContext
        imageStore.discardPromotedCandidate(path)
    }

    fun isStaged(path: String): Boolean = imageStore.isStaged(path)
}
