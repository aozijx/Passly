package com.aozijx.passly.app.entry.favicon

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.platform.media.FaviconCropRequest
import com.aozijx.passly.core.platform.media.FaviconImageProcessor
import com.aozijx.passly.core.platform.media.FaviconProcessingException
import com.aozijx.passly.core.platform.media.FaviconProcessingFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFaviconImageProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val imageDownloader: FaviconImageDownloader,
    private val imageStore: FaviconImageStore,
    private val bitmapTransformer: FaviconBitmapTransformer,
) : FaviconImageProcessor {
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
                    ByteArrayInputStream(imageDownloader.download(value)).use(imageStore::stage)
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
                    val output = imageStore.createTransformOutput()
                    try {
                        bitmapTransformer.transform(input, output, crop)
                        imageStore.finishTransform(input, output).also { transformedPath = it }
                    } catch (error: Throwable) {
                        output.delete()
                        throw error
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
            withContext(NonCancellable + Dispatchers.IO) {
                imageStore.discardPromotedCandidate(promotedPath)
            }
            throw error
        }
    }

    override suspend fun discard(path: String?) = withContext(Dispatchers.IO) {
        path ?: return@withContext
        imageStore.discard(path)
    }

    override suspend fun discardPromotedCandidate(path: String?) = withContext(Dispatchers.IO) {
        path ?: return@withContext
        imageStore.discardPromotedCandidate(path)
    }

    override fun isStaged(path: String): Boolean = imageStore.isStaged(path)

    override fun discardEditorResources(
        stagedPath: String?,
        pendingInputPath: String?,
        promotedCandidatePath: String?,
    ) {
        imageStore.discard(stagedPath)
        imageStore.discard(pendingInputPath)
        imageStore.discardPromotedCandidate(promotedCandidatePath)
    }

    private suspend fun discardAfterCancellation(path: String?) {
        withContext(NonCancellable + Dispatchers.IO) {
            imageStore.discard(path)
        }
    }

    private suspend inline fun <T> resultOfPreservingCancellation(
        block: suspend () -> T,
    ): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error.toFaviconProcessingException())
    }
}

internal fun Throwable.toFaviconProcessingException(): FaviconProcessingException =
    this as? FaviconProcessingException ?: FaviconProcessingException(
        failure = when (this) {
            is FaviconUrlException -> when (reason) {
                FaviconUrlFailure.INVALID_URL,
                FaviconUrlFailure.HTTPS_REQUIRED,
                    -> FaviconProcessingFailure.INVALID_URL

                FaviconUrlFailure.CREDENTIALS_NOT_ALLOWED,
                FaviconUrlFailure.HOST_NOT_ALLOWED,
                FaviconUrlFailure.PRIVATE_ADDRESS,
                    -> FaviconProcessingFailure.URL_NOT_ALLOWED
            }

            is FaviconDownloadException -> when (reason) {
                FaviconDownloadFailure.NOT_IMAGE -> FaviconProcessingFailure.NOT_IMAGE
                FaviconDownloadFailure.TOO_LARGE -> FaviconProcessingFailure.IMAGE_TOO_LARGE
                else -> FaviconProcessingFailure.DOWNLOAD_FAILED
            }

            is FaviconImageException -> when (reason) {
                FaviconImageFailure.TOO_LARGE -> FaviconProcessingFailure.IMAGE_TOO_LARGE
                FaviconImageFailure.SAVE_FAILED -> FaviconProcessingFailure.SAVE_FAILED
                else -> FaviconProcessingFailure.INVALID_IMAGE
            }

            else -> FaviconProcessingFailure.INVALID_IMAGE
        },
        cause = this,
    )
