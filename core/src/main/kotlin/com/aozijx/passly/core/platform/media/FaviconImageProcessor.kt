package com.aozijx.passly.core.platform.media

import android.net.Uri

data class FaviconCropRequest(
    val zoom: Float = MIN_ZOOM,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 6f
    }
}

enum class FaviconProcessingFailure {
    INVALID_URL,
    URL_NOT_ALLOWED,
    DOWNLOAD_FAILED,
    NOT_IMAGE,
    IMAGE_TOO_LARGE,
    INVALID_IMAGE,
    SAVE_FAILED,
}

class FaviconProcessingException(
    val failure: FaviconProcessingFailure,
    cause: Throwable? = null,
) : IllegalStateException(failure.name, cause)

sealed interface FaviconEditorResource {
    val path: String

    data class StagedImage(override val path: String) : FaviconEditorResource

    data class PromotedCandidate(override val path: String) : FaviconEditorResource
}

interface FaviconDraftFiles {
    suspend fun discard(resource: FaviconEditorResource)

    fun discardEditorResources(resources: Set<FaviconEditorResource>)
}

interface FaviconImageProcessor : FaviconDraftFiles {
    suspend fun stageUpload(uri: Uri): Result<String>

    suspend fun stageHttpsUrl(value: String): Result<String>

    suspend fun process(
        stagedInputPath: String,
        crop: FaviconCropRequest?,
    ): Result<String>

    suspend fun promote(stagedPath: String): Result<String>

    fun isStaged(path: String): Boolean
}
