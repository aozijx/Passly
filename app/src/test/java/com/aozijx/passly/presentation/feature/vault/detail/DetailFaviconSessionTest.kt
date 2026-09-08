package com.aozijx.passly.presentation.feature.vault.detail

import com.aozijx.passly.app.entry.favicon.FaviconDraftFiles
import com.aozijx.passly.presentation.ui.vault.detail.model.DetailFaviconEditorUiModel
import com.aozijx.passly.presentation.ui.vault.detail.model.FaviconDraftSourceUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailFaviconSessionTest {

    @Test
    fun `active favicon work rejects a second launch`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val session = DetailFaviconSession(FakeFiles(), this)
        var launches = 0

        session.launch {
            launches++
            gate.await()
        }
        runCurrent()
        session.launch { launches++ }
        runCurrent()

        assertEquals(1, launches)
        gate.complete(Unit)
        runCurrent()
    }

    @Test
    fun `closing editor cancels work and discards every owned path`() = runTest {
        val files = FakeFiles()
        val session = DetailFaviconSession(files, backgroundScope)
        val editor = DetailFaviconEditorUiModel(
            source = FaviconDraftSourceUiModel.PrivateImage("staged"),
            pendingInputPath = "pending",
            promotedCandidatePath = "promoted",
        )

        session.close(editor)

        assertEquals(listOf("staged", "pending", "promoted"), files.closedPaths)
    }

    private class FakeFiles : FaviconDraftFiles {
        val closedPaths = mutableListOf<String?>()

        override suspend fun discard(path: String?) = Unit
        override suspend fun discardPromotedCandidate(path: String?) = Unit

        override fun discardEditorResources(
            stagedPath: String?,
            pendingInputPath: String?,
            promotedCandidatePath: String?,
        ) {
            closedPaths += listOf(stagedPath, pendingInputPath, promotedCandidatePath)
        }
    }
}
