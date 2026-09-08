package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.app.entry.favicon.FaviconDraftFiles
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class DetailFaviconSession(
    private val files: FaviconDraftFiles,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun launch(block: suspend () -> Unit) {
        if (job?.isActive == true) return
        job = scope.launch { block() }
    }

    fun discardReplacedSource(
        editor: DetailFaviconEditorUiModel,
        replacement: FaviconDraftSourceUiModel,
    ) {
        val previous = editor.source
        if (previous is FaviconDraftSourceUiModel.PrivateImage && previous != replacement) {
            scope.launch { files.discard(previous.localPath) }
        }
        editor.promotedCandidatePath?.let { path ->
            scope.launch { files.discardPromotedCandidate(path) }
        }
    }

    fun cancelCrop(pendingInputPath: String?) {
        cancel()
        scope.launch { files.discard(pendingInputPath) }
    }

    fun close(editor: DetailFaviconEditorUiModel) {
        cancel()
        files.discardEditorResources(
            stagedPath = editor.privateImagePath(),
            pendingInputPath = editor.pendingInputPath,
            promotedCandidatePath = editor.promotedCandidatePath,
        )
    }

    private fun cancel() {
        job?.cancel()
        job = null
    }
}
