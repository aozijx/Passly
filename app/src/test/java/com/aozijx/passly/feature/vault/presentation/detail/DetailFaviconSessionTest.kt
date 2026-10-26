package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.platform.media.FaviconDraftFiles
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailFaviconSessionTest {

    @Test
    fun `closing waits for save cancellation to finish before releasing its files`() = runTest {
        val files = FakeFiles()
        val releaseSave = CompletableDeferred<Unit>()
        val session = DetailFaviconSession(files, backgroundScope)
        session.launchSave {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { releaseSave.await() }
            }
        }
        runCurrent()
        session.close(DetailFaviconEditorState.Open(
            initialSource = DetailFaviconSource.InferredDefault,
            promotedCandidatePath = "promoted",
        ))
        runCurrent()
        assertTrue(files.closedPaths.isEmpty())
        releaseSave.complete(Unit)
        runCurrent()
        assertEquals(listOf(null, null, "promoted"), files.closedPaths)
    }

    @Test
    fun `active save rejects image preparation`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val session = DetailFaviconSession(FakeFiles(), backgroundScope)
        session.launchSave { gate.await() }
        runCurrent()
        var prepared = false
        session.launchPreparation { prepared = true }
        runCurrent()
        assertEquals(false, prepared)
        gate.complete(Unit)
    }

    @Test
    fun `active preparation rejects a second preparation`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val session = DetailFaviconSession(FakeFiles(), this)
        var launches = 0

        session.launchPreparation {
            launches++
            gate.await()
        }
        runCurrent()
        session.launchPreparation { launches++ }
        runCurrent()

        assertEquals(1, launches)
        gate.complete(Unit)
        runCurrent()
    }

    @Test
    fun `active preparation does not discard a save`() = runTest {
        val releasePreparation = CompletableDeferred<Unit>()
        val session = DetailFaviconSession(FakeFiles(), this)
        var saved = false

        session.launchPreparation { releasePreparation.await() }
        runCurrent()
        session.launchSave { saved = true }
        runCurrent()

        assertTrue(saved)
        releasePreparation.complete(Unit)
        runCurrent()
    }

    @Test
    fun `active save cannot be replaced`() = runTest {
        val releaseSave = CompletableDeferred<Unit>()
        val session = DetailFaviconSession(FakeFiles(), this)

        session.launchSave { releaseSave.await() }
        runCurrent()

        assertThrows(IllegalStateException::class.java) {
            session.launchSave { }
        }

        releaseSave.complete(Unit)
        runCurrent()
    }

    @Test
    fun `closing editor cancels work and discards every owned path`() = runTest {
        val files = FakeFiles()
        val session = DetailFaviconSession(files, backgroundScope)
        val editor = DetailFaviconEditorState.Open(
            initialSource = DetailFaviconSource.InferredDefault,
            source = DetailFaviconSource.PrivateImage("staged"),
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
