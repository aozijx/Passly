package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.app.entry.favicon.FaviconDraftFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class DetailFaviconSession(
    private val files: FaviconDraftFiles,
    private val scope: CoroutineScope,
) {
    private var preparationJob: Job? = null
    private var saveJob: Job? = null

    fun launchPreparation(block: suspend () -> Unit) {
        if (preparationJob?.isActive == true) return
        preparationJob = scope.launch { block() }
    }

    fun launchSave(block: suspend () -> Unit) {
        check(saveJob?.isActive != true) { "A favicon save is already active" }
        saveJob = scope.launch { block() }
    }

    fun discardReplacedSource(
        editor: DetailFaviconEditorState,
        replacement: DetailFaviconSource,
    ) {
        val previous = editor.source
        if (previous is DetailFaviconSource.PrivateImage && previous != replacement) {
            scope.launch { files.discard(previous.localPath) }
        }
        editor.promotedCandidatePath?.let { path ->
            scope.launch { files.discardPromotedCandidate(path) }
        }
    }

    fun cancelCrop(pendingInputPath: String?) {
        cancelPreparation()
        scope.launch { files.discard(pendingInputPath) }
    }

    fun close(editor: DetailFaviconEditorState) {
        cancelAll()
        files.discardEditorResources(
            stagedPath = editor.privateImagePath(),
            pendingInputPath = editor.pendingInputPath,
            promotedCandidatePath = editor.promotedCandidatePath,
        )
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
