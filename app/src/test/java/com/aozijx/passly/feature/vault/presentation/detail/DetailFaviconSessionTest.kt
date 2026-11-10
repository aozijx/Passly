package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.platform.media.FaviconDraftFiles
import com.aozijx.passly.core.platform.media.FaviconEditorResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailFaviconSessionTest {
    @Test
    fun `cancel crop finishes discarding input when owner scope is cancelled during IO`() =
        runTest {
            val files = FakeFiles()
            val deletionStarted = CompletableDeferred<Unit>()
            val releaseDeletion = CompletableDeferred<Unit>()
            files.onDiscard = {
                deletionStarted.complete(Unit)
                releaseDeletion.await()
            }
            val ownerJob = Job()
            val ownerScope = CoroutineScope(ownerJob + StandardTestDispatcher(testScheduler))
            val session = DetailFaviconSession(files, ownerScope)

            session.cancelCrop(DetailFaviconCropInput.Ready("pending-input"))
            deletionStarted.await()
            ownerJob.cancel()
            releaseDeletion.complete(Unit)
            runCurrent()

            assertEquals(listOf(FaviconEditorResource.StagedImage("pending-input")), files.discardedResources)
        }

    @Test
    fun `publishing a new crop input only releases the previous pending input`() =
        runTest {
            val files = FakeFiles()
            val session = DetailFaviconSession(files, backgroundScope)
            session.trackOwnedResource(FaviconEditorResource.StagedImage("old-input"))
            session.trackOwnedResource(FaviconEditorResource.PromotedCandidate("candidate"))
            val editor =
                DetailFaviconEditorState.Open(
                    initialSource = DetailFaviconSource.InferredDefault,
                    source = DetailFaviconSource.PrivateImage("current-source"),
                    cropInput = DetailFaviconCropInput.Ready("old-input"),
                )
            session.launchPreparation { token ->
                session.publishPreparation(
                    token,
                    Result.success("new-input"),
                    editor,
                    DetailFaviconSession.PreparedImageUse.CROP_INPUT,
                    onSuccess = { },
                    onFailure = { error("unexpected failure") },
                )
            }
            runCurrent()
            assertEquals(setOf(FaviconEditorResource.StagedImage("old-input")), files.closedResources.toSet())
        }

    @Test
    fun `cancellation returned by a processor is propagated rather than published as failure`() =
        runTest {
            val session = DetailFaviconSession(FakeFiles(), backgroundScope)
            val editor = DetailFaviconEditorState.Open(initialSource = DetailFaviconSource.InferredDefault)
            var propagated = false
            session.launchPreparation { token ->
                try {
                    session.publishPreparation(
                        token,
                        Result.failure(CancellationException()),
                        editor,
                        DetailFaviconSession.PreparedImageUse.CROP_INPUT,
                        onSuccess = { error("unexpected success") },
                        onFailure = { error("unexpected failure") },
                    )
                } catch (expected: CancellationException) {
                    propagated = true
                }
            }
            runCurrent()
            assertTrue(propagated)
        }

    @Test
    fun `late successful preparation is discarded after selecting another source`() =
        runTest {
            val files = FakeFiles()
            val gate = CompletableDeferred<Unit>()
            val session = DetailFaviconSession(files, backgroundScope)
            val editor = DetailFaviconEditorState.Open(initialSource = DetailFaviconSource.InferredDefault)
            var published = false
            session.launchPreparation { token ->
                withContext(NonCancellable) { gate.await() }
                session.publishPreparation(
                    token,
                    Result.success("late-image"),
                    editor,
                    DetailFaviconSession.PreparedImageUse.SOURCE,
                    onSuccess = { published = true },
                    onFailure = { error("unexpected failure") },
                )
            }
            runCurrent()
            session.discardReplacedSource(DetailFaviconSource.InferredDefault)
            gate.complete(Unit)
            runCurrent()
            assertEquals(false, published)
            assertTrue(FaviconEditorResource.StagedImage("late-image") in files.closedResources)
        }

    @Test
    fun `late failure from a closed editor cannot change a new preparation`() =
        runTest {
            val files = FakeFiles()
            val gate = CompletableDeferred<Unit>()
            val session = DetailFaviconSession(files, backgroundScope)
            val editor = DetailFaviconEditorState.Open(initialSource = DetailFaviconSource.InferredDefault)
            var failed = false
            session.launchPreparation { token ->
                withContext(NonCancellable) { gate.await() }
                session.publishPreparation(
                    token,
                    Result.failure(IllegalStateException("fixture")),
                    editor,
                    DetailFaviconSession.PreparedImageUse.CROP_INPUT,
                    onSuccess = { error("unexpected success") },
                    onFailure = { failed = true },
                )
            }
            runCurrent()
            session.close()
            var published = false
            session.launchPreparation { token ->
                session.publishPreparation(
                    token,
                    Result.success("current-image"),
                    editor,
                    DetailFaviconSession.PreparedImageUse.CROP_INPUT,
                    onSuccess = { published = true },
                    onFailure = { error("unexpected failure") },
                )
            }
            runCurrent()
            gate.complete(Unit)
            runCurrent()
            assertTrue(published)
            assertEquals(false, failed)
            assertTrue(FaviconEditorResource.StagedImage("current-image") !in files.closedResources)
        }

    @Test
    fun `publishing prepared source releases replaced input and candidate but retains new image`() =
        runTest {
            val files = FakeFiles()
            val session = DetailFaviconSession(files, backgroundScope)
            session.trackOwnedResource(FaviconEditorResource.StagedImage("old-image"))
            session.trackOwnedResource(FaviconEditorResource.StagedImage("old-input"))
            session.trackOwnedResource(FaviconEditorResource.PromotedCandidate("old-candidate"))
            val editor =
                DetailFaviconEditorState.Open(
                    initialSource = DetailFaviconSource.InferredDefault,
                    source = DetailFaviconSource.PrivateImage("old-image"),
                    cropInput = DetailFaviconCropInput.Ready("old-input"),
                )
            session.launchPreparation { token ->
                session.publishPreparation(
                    token,
                    Result.success("new-image"),
                    editor,
                    DetailFaviconSession.PreparedImageUse.SOURCE,
                    onSuccess = { },
                    onFailure = { error("unexpected failure") },
                )
            }
            runCurrent()
            assertEquals(
                setOf(
                    FaviconEditorResource.StagedImage("old-image"),
                    FaviconEditorResource.StagedImage("old-input"),
                    FaviconEditorResource.PromotedCandidate("old-candidate"),
                ),
                files.closedResources.toSet(),
            )
        }

    @Test
    fun `replacing a source cancels preparation and releases abandoned crop input`() =
        runTest {
            val files = FakeFiles()
            val session = DetailFaviconSession(files, backgroundScope)
            session.trackOwnedResource(FaviconEditorResource.StagedImage("crop-input"))
            var cancelled = false
            session.launchPreparation {
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
            runCurrent()
            session.discardReplacedSource(DetailFaviconSource.InferredDefault)
            runCurrent()
            assertTrue(cancelled)
            assertTrue(FaviconEditorResource.StagedImage("crop-input") in files.closedResources)
        }

    @Test
    fun `closing waits for save cancellation to finish before releasing its files`() =
        runTest {
            val files = FakeFiles()
            val releaseSave = CompletableDeferred<Unit>()
            val session = DetailFaviconSession(files, backgroundScope)
            session.trackOwnedResource(FaviconEditorResource.PromotedCandidate("promoted"))
            session.launchSave {
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) { releaseSave.await() }
                }
            }
            runCurrent()
            session.close()
            runCurrent()
            assertTrue(files.closedResources.isEmpty())
            releaseSave.complete(Unit)
            runCurrent()
            assertEquals(setOf(FaviconEditorResource.PromotedCandidate("promoted")), files.closedResources.toSet())
        }

    @Test
    fun `active save rejects image preparation`() =
        runTest {
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
    fun `active preparation rejects a second preparation`() =
        runTest {
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
    fun `active preparation does not discard a save`() =
        runTest {
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
    fun `active save cannot be replaced`() =
        runTest {
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
    fun `closing editor releases remaining resources after commit transfer`() =
        runTest {
            val files = FakeFiles()
            val session = DetailFaviconSession(files, backgroundScope)
            session.trackOwnedResource(FaviconEditorResource.StagedImage("staged"))
            session.trackOwnedResource(FaviconEditorResource.StagedImage("pending"))
            session.trackOwnedResource(FaviconEditorResource.PromotedCandidate("promoted"))
            session.promotedCandidateCommitted("promoted")

            session.close()

            assertEquals(
                setOf(
                    FaviconEditorResource.StagedImage("staged"),
                    FaviconEditorResource.StagedImage("pending"),
                ),
                files.closedResources.toSet(),
            )
        }

    private class FakeFiles : FaviconDraftFiles {
        val closedResources = mutableListOf<FaviconEditorResource>()
        val discardedResources = mutableListOf<FaviconEditorResource>()
        var onDiscard: suspend (FaviconEditorResource) -> Unit = {}

        override suspend fun discard(resource: FaviconEditorResource) {
            onDiscard(resource)
            discardedResources += resource
        }

        override fun discardEditorResources(resources: Set<FaviconEditorResource>) {
            closedResources += resources
        }
    }
}
