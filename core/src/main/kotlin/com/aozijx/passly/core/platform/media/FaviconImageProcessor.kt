package com.aozijx.passly.core.platform.media

import android.net.Uri

data class FaviconCropRequest(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

interface FaviconDraftFiles {
    suspend fun discard(path: String?)
    suspend fun discardPromotedCandidate(path: String?)
    fun discardEditorResources(
        stagedPath: String?,
        pendingInputPath: String?,
        promotedCandidatePath: String?,
    )
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
