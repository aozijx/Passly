package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.platform.media.FaviconDraftFiles
import com.aozijx.passly.core.platform.media.FaviconEditorResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DetailFaviconSession(
    private val files: FaviconDraftFiles,
    private val scope: CoroutineScope,
) {
    enum class PreparedImageUse { CROP_INPUT, SOURCE }

    private val resourceLock = Any()
    private val ownedResources = linkedSetOf<FaviconEditorResource>()
    private var preparationJob: Job? = null
    private var saveJob: Job? = null

    val isSaving: Boolean
        get() = saveJob?.isCompleted == false

    fun launchPreparation(block: suspend (Job) -> Unit) {
        if (isSaving || preparationJob?.isActive == true) return
        lateinit var job: Job
        job = scope.launch(start = CoroutineStart.LAZY) { block(job) }
        preparationJob = job
        job.start()
    }

    suspend fun publishPreparation(
        token: Job,
        result: Result<String>,
        editor: DetailFaviconEditorState.Open,
        use: PreparedImageUse,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        val path = result.getOrNull()
        if (preparationJob !== token || !currentCoroutineContext().isActive) {
            path?.let { files.discardEditorResources(setOf(FaviconEditorResource.StagedImage(it))) }
            return
        }
        try {
            result.fold(
                onSuccess = { newPath ->
                    own(FaviconEditorResource.StagedImage(newPath))
                    when (use) {
                        PreparedImageUse.CROP_INPUT -> {
                            when (val input = editor.cropInput) {
                                DetailFaviconCropInput.None -> {
                                    Unit
                                }

                                is DetailFaviconCropInput.Ready -> {
                                    if (input.path != newPath) {
                                        discardOwned(FaviconEditorResource.StagedImage(input.path))
                                    }
                                }
                            }
                        }

                        PreparedImageUse.SOURCE -> {
                            releaseOwnedResourcesExcept(DetailFaviconSource.PrivateImage(newPath))
                        }
                    }
                    onSuccess(newPath)
                },
                onFailure = onFailure,
            )
        } catch (failure: Throwable) {
            path?.let { discardOwned(FaviconEditorResource.StagedImage(it)) }
            throw failure
        }
    }

    fun launchSave(block: suspend () -> Unit) {
        check(!isSaving) { "A favicon save is already active" }
        saveJob = scope.launch { block() }
    }

    fun discardReplacedSource(replacement: DetailFaviconSource) {
        cancelPreparation()
        releaseOwnedResourcesExcept(replacement)
    }

    fun stagedImagePromoted(
        stagedPath: String,
        promotedPath: String,
    ) {
        synchronized(resourceLock) {
            ownedResources.remove(FaviconEditorResource.StagedImage(stagedPath))
            ownedResources.add(FaviconEditorResource.PromotedCandidate(promotedPath))
        }
    }

    fun trackOwnedResource(resource: FaviconEditorResource) {
        own(resource)
    }

    fun promotedCandidateCommitted(path: String) {
        synchronized(resourceLock) {
            ownedResources.remove(FaviconEditorResource.PromotedCandidate(path))
        }
    }

    suspend fun discardPromotedCandidate(path: String) {
        forget(FaviconEditorResource.PromotedCandidate(path))
        withContext(NonCancellable) {
            files.discard(FaviconEditorResource.PromotedCandidate(path))
        }
    }

    fun cancelCrop(input: DetailFaviconCropInput) {
        cancelPreparation()
        val path =
            when (input) {
                DetailFaviconCropInput.None -> return
                is DetailFaviconCropInput.Ready -> input.path
            }
        val resource = FaviconEditorResource.StagedImage(path)
        forget(resource)
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { files.discard(resource) }
        }
    }

    fun close() {
        val pendingSave = saveJob
        cancelAll()
        val release = ::discardAllOwnedResources
        // A cancelled writer may still be committing or rolling back. Do not reclaim its input early.
        if (pendingSave == null) release() else pendingSave.invokeOnCompletion { release() }
    }

    private fun own(resource: FaviconEditorResource) {
        synchronized(resourceLock) {
            ownedResources.add(resource)
        }
    }

    private fun forget(resource: FaviconEditorResource) {
        synchronized(resourceLock) {
            ownedResources.remove(resource)
        }
    }

    private fun discardOwned(resource: FaviconEditorResource) {
        val wasOwned =
            synchronized(resourceLock) {
                ownedResources.remove(resource)
            }
        if (wasOwned) files.discardEditorResources(setOf(resource))
    }

    private fun releaseOwnedResourcesExcept(replacement: DetailFaviconSource) {
        val abandoned =
            synchronized(resourceLock) {
                ownedResources
                    .filterNot { resource ->
                        replacement is DetailFaviconSource.PrivateImage && resource.path == replacement.localPath
                    }.toSet()
                    .also(ownedResources::removeAll)
            }
        if (abandoned.isNotEmpty()) files.discardEditorResources(abandoned)
    }

    private fun discardAllOwnedResources() {
        val abandoned =
            synchronized(resourceLock) {
                ownedResources.toSet().also { ownedResources.clear() }
            }
        if (abandoned.isNotEmpty()) files.discardEditorResources(abandoned)
    }

    private fun cancelPreparation() {
        preparationJob?.cancel()
        preparationJob = null
    }

    private fun cancelAll() {
        cancelPreparation()
        saveJob?.cancel()
        saveJob = null
    }
}
