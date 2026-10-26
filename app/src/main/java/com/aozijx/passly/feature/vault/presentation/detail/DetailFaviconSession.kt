package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.platform.media.FaviconDraftFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class DetailFaviconSession(
    private val files: FaviconDraftFiles,
    private val scope: CoroutineScope,
) {
    sealed interface Preparation {
        data object Closed : Preparation
        class Active internal constructor() : Preparation
    }

    enum class PreparedImageUse { CROP_INPUT, SOURCE }

    private var preparation: Preparation = Preparation.Closed
    private var preparationJob: Job? = null
    private var saveJob: Job? = null

    val isSaving: Boolean
        get() = saveJob?.isCompleted == false

    fun launchPreparation(block: suspend (Preparation.Active) -> Unit) {
        if (isSaving || preparationJob?.isActive == true) return
        val token = Preparation.Active()
        preparation = token
        preparationJob = scope.launch { block(token) }
    }

    suspend fun publishPreparation(
        token: Preparation.Active,
        result: Result<String>,
        editor: DetailFaviconEditorState.Open,
        use: PreparedImageUse,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        val path = result.getOrNull()
        if (preparation !== token || !currentCoroutineContext().isActive) {
            if (path != null) files.discardEditorResources(path, null, null)
            return
        }
        try {
            result.fold(
                onSuccess = { newPath ->
                    when (use) {
                        PreparedImageUse.CROP_INPUT -> files.discardEditorResources(
                            stagedPath = null,
                            pendingInputPath = editor.pendingInputPath?.takeUnless { it == newPath },
                            promotedCandidatePath = null,
                        )
                        PreparedImageUse.SOURCE -> releaseReplacedSource(editor, DetailFaviconSource.PrivateImage(newPath))
                    }
                    onSuccess(newPath)
                },
                onFailure = onFailure,
            )
        } catch (failure: Throwable) {
            if (path != null) files.discardEditorResources(path, null, null)
            throw failure
        }
    }

    fun launchSave(block: suspend () -> Unit) {
        check(!isSaving) { "A favicon save is already active" }
        saveJob = scope.launch { block() }
    }

    fun discardReplacedSource(
        editor: DetailFaviconEditorState.Open,
        replacement: DetailFaviconSource,
    ) {
        cancelPreparation()
        releaseReplacedSource(editor, replacement)
    }

    private fun releaseReplacedSource(editor: DetailFaviconEditorState.Open, replacement: DetailFaviconSource) {
        val retained = (replacement as? DetailFaviconSource.PrivateImage)?.localPath
        files.discardEditorResources(
            stagedPath = editor.privateImagePath()?.takeUnless { it == retained },
            pendingInputPath = editor.pendingInputPath?.takeUnless { it == retained },
            promotedCandidatePath = editor.promotedCandidatePath?.takeUnless { it == retained },
        )
    }

    fun cancelCrop(pendingInputPath: String?) {
        cancelPreparation()
        scope.launch { files.discard(pendingInputPath) }
    }

    fun close(editor: DetailFaviconEditorState.Open) {
        val pendingSave = saveJob
        cancelAll()
        val release = {
            files.discardEditorResources(
                stagedPath = editor.privateImagePath(),
                pendingInputPath = editor.pendingInputPath,
                promotedCandidatePath = editor.promotedCandidatePath,
            )
        }
        // A cancelled writer may still be committing or rolling back. Do not reclaim its input early.
        if (pendingSave == null) release() else pendingSave.invokeOnCompletion { release() }
    }

    private fun cancelPreparation() {
        preparation = Preparation.Closed
        preparationJob?.cancel()
        preparationJob = null
    }

    private fun cancelAll() {
        cancelPreparation()
        saveJob?.cancel()
        saveJob = null
    }
}
