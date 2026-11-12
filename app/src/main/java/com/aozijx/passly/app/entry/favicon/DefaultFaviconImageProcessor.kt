package com.aozijx.passly.app.entry.favicon

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.platform.media.FaviconCropRequest
import com.aozijx.passly.core.platform.media.FaviconEditorResource
import com.aozijx.passly.core.platform.media.FaviconImageProcessor
import com.aozijx.passly.core.platform.media.FaviconProcessingException
import com.aozijx.passly.core.platform.media.FaviconProcessingFailure
import com.aozijx.passly.domain.entry.port.EntryIconResourceCleaner
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@Singleton
class DefaultFaviconImageProcessor
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val imageDownloader: FaviconImageDownloader,
        private val imageStore: FaviconImageStore,
        private val bitmapTransformer: FaviconBitmapTransformer,
        private val iconResourceCleaner: EntryIconResourceCleaner,
    ) : FaviconImageProcessor {
        init {
            imageStore.recoveredPromotedCandidates.forEach(iconResourceCleaner::discardUnreferencedIcon)
        }

        override suspend fun stageUpload(uri: Uri): Result<String> {
            var stagedPath: String? = null
            return try {
                withContext(Dispatchers.IO) {
                    resultOfPreservingCancellation {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            imageStore.stage(input).also { stagedPath = it }
                        } ?: throw FaviconImageException(FaviconImageFailure.DECODE_FAILED)
                    }
                }
            } catch (error: CancellationException) {
                discardAfterCancellation(stagedPath)
                throw error
            }
        }

        override suspend fun stageHttpsUrl(value: String): Result<String> {
            var stagedPath: String? = null
            return try {
                withContext(Dispatchers.IO) {
                    resultOfPreservingCancellation {
                        ByteArrayInputStream(imageDownloader.download(value))
                            .use(imageStore::stage)
                            .also { stagedPath = it }
                    }
                }
            } catch (error: CancellationException) {
                discardAfterCancellation(stagedPath)
                throw error
            }
        }

        override suspend fun process(
            stagedInputPath: String,
            crop: FaviconCropRequest?,
        ): Result<String> {
            var transformedPath: String? = null
            return try {
                withContext(Dispatchers.IO) {
                    resultOfPreservingCancellation {
                        val input = imageStore.requireStaged(stagedInputPath)
                        val output = imageStore.createTransformOutput(bitmapTransformer.outputExtension(input, crop))
                        withFaviconOutputCleanup(output) {
                            bitmapTransformer.transform(input, output, crop)
                            imageStore.finishTransform(input, output).also {
                                transformedPath = it
                            }
                        }
                    }
                }
            } catch (error: CancellationException) {
                discardAfterCancellation(transformedPath)
                throw error
            }
        }

        override suspend fun promote(stagedPath: String): Result<String> {
            var promotedPath: String? = null
            return try {
                withContext(Dispatchers.IO) {
                    resultOfPreservingCancellation {
                        imageStore.promote(stagedPath).also { promotedPath = it }
                    }
                }
            } catch (error: CancellationException) {
                promotedPath?.let(iconResourceCleaner::discardUnreferencedIcon)
                throw error
            }
        }

        override suspend fun discard(resource: FaviconEditorResource) {
            withContext(Dispatchers.IO) {
                discardResource(resource)
            }
        }

        override fun isStaged(path: String): Boolean = imageStore.isStaged(path)

        override fun discardEditorResources(resources: Set<FaviconEditorResource>) {
            resources.forEach(::discardResource)
        }

        private fun discardResource(resource: FaviconEditorResource) {
            when (resource) {
                is FaviconEditorResource.StagedImage -> imageStore.discard(resource.path)
                is FaviconEditorResource.PromotedCandidate -> iconResourceCleaner.discardUnreferencedIcon(resource.path)
            }
        }

        private suspend fun discardAfterCancellation(path: String?) {
            path?.let {
                withContext(NonCancellable + Dispatchers.IO) {
                    imageStore.discard(it)
                }
            }
        }
    }

internal inline fun <T> withFaviconOutputCleanup(
    output: File,
    block: () -> T,
): T =
    try {
        block()
    } catch (error: Throwable) {
        cleanupFaviconFileAfterFailure(output, error)
        throw error
    }

internal fun cleanupFaviconFileAfterFailure(
    file: File,
    originalFailure: Throwable,
) {
    try {
        file.delete()
    } catch (cleanupFailure: Throwable) {
        val preferredFailure = preferFaviconCleanupFailure(originalFailure, cleanupFailure)
        if (preferredFailure !== originalFailure) throw preferredFailure
    }
}

private fun preferFaviconCleanupFailure(
    originalFailure: Throwable,
    cleanupFailure: Throwable,
): Throwable {
    if (originalFailure === cleanupFailure) return originalFailure
    if (cleanupFailure.faviconFailurePriority() > originalFailure.faviconFailurePriority()) {
        cleanupFailure.addSuppressed(originalFailure)
        return cleanupFailure
    }
    originalFailure.addSuppressed(cleanupFailure)
    return originalFailure
}

private fun Throwable.faviconFailurePriority(): Int =
    when (this) {
        is Error -> 2
        is CancellationException -> 1
        else -> 0
    }

internal suspend fun <T> resultOfPreservingCancellation(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error.toFaviconProcessingException())
    }

internal fun Throwable.toFaviconProcessingException(): FaviconProcessingException =
    this as? FaviconProcessingException ?: FaviconProcessingException(
        failure =
            when (this) {
                is FaviconUrlException -> {
                    when (reason) {
                        FaviconUrlFailure.INVALID_URL,
                        FaviconUrlFailure.HTTPS_REQUIRED,
                        -> FaviconProcessingFailure.INVALID_URL

                        FaviconUrlFailure.CREDENTIALS_NOT_ALLOWED,
                        FaviconUrlFailure.HOST_NOT_ALLOWED,
                        FaviconUrlFailure.PRIVATE_ADDRESS,
                        -> FaviconProcessingFailure.URL_NOT_ALLOWED
                    }
                }

                is FaviconDownloadException -> {
                    when (reason) {
                        FaviconDownloadFailure.NOT_IMAGE -> FaviconProcessingFailure.NOT_IMAGE
                        FaviconDownloadFailure.TOO_LARGE -> FaviconProcessingFailure.IMAGE_TOO_LARGE
                        else -> FaviconProcessingFailure.DOWNLOAD_FAILED
                    }
                }

                is FaviconImageException -> {
                    when (reason) {
                        FaviconImageFailure.TOO_LARGE -> FaviconProcessingFailure.IMAGE_TOO_LARGE
                        FaviconImageFailure.SAVE_FAILED -> FaviconProcessingFailure.SAVE_FAILED
                        else -> FaviconProcessingFailure.INVALID_IMAGE
                    }
                }

                else -> {
                    FaviconProcessingFailure.INVALID_IMAGE
                }
            },
        cause = this,
    )
